package model;

import model.execution.*;
import model.message.Message;
import model.message.MessageHeader;
import model.message.MessageType;

import java.nio.ByteBuffer;
import java.util.*;

public class Node {

    public static final int HELLO_INTERVAL = 20;
    public static final int ROUTING_INTERVAL = 200;
    public static final int TRACING_PERIOD = 10;
    public static final int JOIN_VOLLEY_AMOUNT = 10;
    public static final int INVITE_VOLLEY_AMOUNT = 5;

    private static final String TRANSMITTER_COOLDOWN_EVENT_KEY = "tc";

    private final long serialId;
    private byte nodeId = -1;
    private NodeStatus status = NodeStatus.Down;
    boolean isController = false;
    private ChannelInfo meshChannel;
    private final Set<Byte> routingRegistry = new HashSet<>();

    private RetxRegister retxRegister;
    private final Map<String, Integer> joinCounter = new HashMap<>();
    private final Map<Integer, Integer> traceCounter = new HashMap<>();

    private CorrespondenceClient uplink;
    private CorrespondenceClient hello;

    private final MessageCache cache = new MessageCache(64);
    private final EventHandler handler = new EventHandler(this);
    private final LoRaMeshClient meshClient;
    private final PceClient pceClient;
    private final DataSinkClient dataSinkClient;
    private final Logger logger;

    public Node(long serialId, LoRaMeshClient meshClient, DataSinkClient dataSinkClient, PceClient pceClient, Logger logger) {
        this.serialId = serialId;
        this.meshClient = meshClient;
        this.dataSinkClient = dataSinkClient;
        this.pceClient = pceClient;
        this.logger = logger;
    }

    public boolean isAlive() {
        return status != NodeStatus.Down && status != NodeStatus.Error;
    }

    public void statusCheck() {
        // todo is rebooting necessary?
        switch (status) {
            case Controller -> {
                meshChannel = pceClient.heartbeat();
                if (meshChannel == null) error("disconnected");
            }
            case Node -> {
                var result = pceClient.heartbeat();
                if (result != null) error("connected");
            }
        }
    }

    public void debug(String format, Object... args) {
        if (logger == null) return;
        logger.debug(String.format(format, args), this);
    }

    public void info(String format, Object... args) {
        if (logger == null) return;
        logger.info(String.format(format, args), this);
    }

    public void warn(String format, Object... args) {
        if (logger == null) return;
        logger.warn(String.format(format, args), this);
    }

    public void error(String format, Object... args) {
        handler.abortAndReset();
        status = NodeStatus.Error;
        if (logger == null) return;
        logger.error(String.format(format, args), this);
        handler.scheduled("recover", System.currentTimeMillis() + 1000)
                .then(this::wake);
    }

    public void wake() {
        if (isAlive()) {
            warn("wake up called on live node");
            return;
        } else {
            debug("wake up");
        }

        meshChannel = pceClient.heartbeat();
        if (meshChannel != null) {
            isController = true;
            initController(pceClient.allocateNodeId());
        } else {
            seek();
        }
    }

    public void refresh() {
        synchronized (handler) {
            handler.notifyAll();
        }
    }

    private boolean shouldForward(Message message) {
        return routingRegistry.contains(message.getRoutingAddress())
                && (MessageType.Resend.matches(message) || !cache.contains(message));
    }

    private void send(Message message) {
        if (MessageType.Hello.matches(message)
                || MessageType.Trace.matches(message)
                || MessageType.Resend.matches(message)) {
            debug("sending uncached %s", message);
            meshClient.enqueue(meshChannel, message);
        } else if (MessageType.Upwards.matches(message) && status == NodeStatus.Controller) {
            debug("to api: %s", message);
            var commands = pceClient.receive(this.nodeId, message);
            debug("api answered: %s", commands);
            for (var command : commands) interpretCommand(command);
        } else {
            info("sending %s", message);
            cache.store(message);
            meshClient.enqueue(meshChannel, message);
        }
    }

    private void seek() {
        debug("entering seek mode...");
        nodeId = -1;
        status = NodeStatus.Seeking;

        meshClient.listen(ChannelInfo.rendezvous, new Observer<>() {
            boolean disposed = false;

            @Override
            public void next(Message message) {
                meshChannel = message.dataAsChannelInfo();
                join();
            }

            @Override
            public boolean isExpired() {
                return disposed ||  nodeId != -1;
            }

            @Override
            public void dispose() {
                disposed = true;
            }
        });
    }

    private void join() {
        debug("entering join mode...");
        nodeId = 0;
        status = NodeStatus.Joining;

        // construct join message
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + 1);
        buffer.put((byte) 0);
        buffer.putLong(serialId);
        byte[] data =  buffer.array();

        hello = LocalCorrespondenceClient.from(nodeId);

        var that = this;
        meshClient.listen(meshChannel, new Observer<>() {
            boolean disposed = false;
            @Override
            public void next(Message message) {
                if (MessageType.DownwardsJoin.matches(message) && message.hasData()) {
                    ByteBuffer buffer = ByteBuffer.wrap(message.data());
                    byte assignedId = buffer.get();
                    long serialId = buffer.getLong();
                    if (serialId == that.serialId) {
                        dispose();
                        initNode(assignedId);
                    }
                }
            }

            @Override
            public boolean isExpired() {
                return disposed;
            }

            @Override
            public void dispose() {
                disposed = true;
            }
        });

        for (int i = 0; i < JOIN_VOLLEY_AMOUNT; i++) {
            send(hello.packAndIncrement(MessageType.Hello, data));
        }
    }

    private void initNode(byte assignedId) {
        info("init as node %d", assignedId);
        this.nodeId = assignedId;
        hello = LocalCorrespondenceClient.from(this.nodeId);
        uplink = LocalCorrespondenceClient.from(this.nodeId);
        retxRegister = new RetxRegisterImpl();
        status = NodeStatus.Node;

        handler.when(TRANSMITTER_COOLDOWN_EVENT_KEY)
                .then(() -> send(generateHello()))
                .labelled("hello")
                .reactEvery(HELLO_INTERVAL);

        handler.when(TRANSMITTER_COOLDOWN_EVENT_KEY)
                .then(() -> send(generateNetworkData()))
                .labelled("routing")
                .reactEvery(ROUTING_INTERVAL);

        meshClient.listen(meshChannel, new Observer<>() {
            boolean disposed = false;
            @Override
            public void next(Message message) {
                handleMessageAsNode(message);
            }

            @Override
            public boolean isExpired() {
                return disposed || status != NodeStatus.Node || nodeId != assignedId;
            }

            @Override
            public void dispose() {
                disposed = true;
            }
        });
    }

    private void initController(byte assignedId) {
        info("init as controller %d", assignedId);
        this.nodeId = assignedId;
        hello = LocalCorrespondenceClient.from(this.nodeId);
        uplink = LocalCorrespondenceClient.from(this.nodeId);
        retxRegister = new RetxRegisterImpl();
        status = NodeStatus.Controller;

        handler.when(TRANSMITTER_COOLDOWN_EVENT_KEY)
                .then(() -> send(generateHello()))
                .labelled("hello")
                .reactEvery(HELLO_INTERVAL);

        handler.when(TRANSMITTER_COOLDOWN_EVENT_KEY)
                .then(() -> send(generateNetworkData()))
                .labelled("routing")
                .reactEvery(ROUTING_INTERVAL);

        meshClient.listen(meshChannel, new Observer<>() {
            boolean disposed = false;
            @Override
            public void next(Message message) {
                handleMessageAsController(message);
            }

            @Override
            public boolean isExpired() {
                return disposed || status != NodeStatus.Controller || nodeId != assignedId;
            }

            @Override
            public void dispose() {
                disposed = true;
            }
        });
    }

    private void  handleMessageAsController(Message message) {
        if (MessageType.Hello.matches(message)) {
            handleHello(message);
        } else if (MessageType.Trace.matches(message)) {
            handleTrace(message);
        } else {
            debug("to api: %s", message);
            var commands = pceClient.receive(this.nodeId, message);
            debug("api answered: %s", commands);
            for (var command : commands) interpretCommand(command);
        }
    }

    private void handleMessageAsNode(Message message) {
        switch (MessageType.pure(message)) {
            case Hello -> handleHello(message);
            case Join -> handleJoin(message);
            case Routing -> handleRouting(message);
            case Trace -> handleTrace(message);
            default -> handleDefaultAsNode(message);
        }
    }

    private void handleHello(Message message) {
        debug("received hello: %s", message);
        if (message.getNodeId() == nodeId) {
            warn("received own hello");
        } else if (message.getNodeId() == 0) {
            var code = message.dataAsString(1);


            joinCounter.compute(code, (k, v) -> {

                if (v == null) {
                    handler.delayed("join " + code, () -> meshClient.getSendingIntervalMillis() * JOIN_VOLLEY_AMOUNT * 2)
                            .then(() -> {
                                byte reliability = (byte) (255f * joinCounter.remove(code) / JOIN_VOLLEY_AMOUNT);
                                byte[] data = message.data();
                                data[0] = reliability;
                                send(uplink.packAndIncrement(MessageType.UpwardsJoin, data));
                            });
                    return 1;
                } else {
                    return v + 1;
                }
            });
        } else {
            retxRegister.next(message);

            if (message.hasData()) {
                for (int i = 0; i + 1 < message.dataLength(); i += 2) {
                    int tracingHeader = 0;
                    tracingHeader |= message.data(i) << MessageHeader.ADDRESS_SHIFT;
                    tracingHeader |= message.data(i + 1) << MessageHeader.COUNTER_SHIFT;
                    traceCounter.putIfAbsent(tracingHeader, 0);
                    Message restored = cache.restore(tracingHeader);
                    if ((tracingHeader & MessageHeader.RESOLVED_BIT) != 0) {
                        traceCounter.remove(tracingHeader & ~MessageHeader.RESOLVED_BIT);
                    } else if (restored != null) {
                        traceCounter.remove(tracingHeader & ~MessageHeader.RESOLVED_BIT);
                        handler.immediate("resend " + restored)
                                .then(() -> send(restored));
                    }
                }
            }
        }
    }

    private void handleTrace(Message message) {
        debug("received trace: %s", message);

        Collection<Byte> uncached = new ArrayList<>();
        for (byte correspondenceCounter : message.data()) {
            Message restored = cache.restore(message.getTracingHeader(correspondenceCounter));
            if (restored != null) {
                send(restored);
            } else {
                uncached.add(correspondenceCounter);
            }
        }

        if (!uncached.isEmpty() && message.getNodeId() != nodeId) {
            byte[] data = new byte[uncached.size()];
            int i = 0;
            for (byte b : uncached) data[i++] = b;
            handleDefaultAsNode(new Message(message.header(), data));
        }
    }

    private void handleJoin(Message message) {
        debug("received join: %s", message);
        if (MessageType.Downwards.matches(message) && message.getNodeId() == nodeId) {
            registerMessage(message);
            byte assignedId = message.data(0);
            routingRegistry.add(assignedId);
            routingRegistry.add((byte) (assignedId | MessageHeader.DOWNWARDS_BIT));

            handler.when(TRANSMITTER_COOLDOWN_EVENT_KEY)
                    .then(() -> send(message))
                    .labelled("invite " + assignedId)
                    .reactEvery(HELLO_INTERVAL + 1)
                    .invalidateIf(() -> retxRegister.knows(assignedId));

        } else if (MessageType.Downwards.matches(message) && shouldForward(message)) {
            routingRegistry.add(message.data(0));
            routingRegistry.add((byte) (message.data(0) | MessageHeader.DOWNWARDS_BIT));
            send(message);
        } else {
            handleDefaultAsNode(message);
        }
    }

    private void handleRouting(Message message) {
        debug("received routing: %s", message);
        if (MessageType.Downwards.matches(message) && message.getNodeId() == nodeId) {
            registerMessage(message);
            updateRouting(message.data());
        } else {
            handleDefaultAsNode(message);
        }
    }

    private void handleDefaultAsNode(Message message) {
        debug("received message: %s", message);
        cache.store(message);
        if (shouldForward(message)) {
            send(message);
        }
    }

    private void registerMessage(Message message) {
        var lost = uplink.registerAndListLosses(message);
        for (byte b : lost) {
            int tracingHeader = 0;
            tracingHeader |= MessageHeader.DOWNWARDS_BIT;
            tracingHeader |= nodeId << MessageHeader.ADDRESS_SHIFT;
            tracingHeader |= b << MessageHeader.COUNTER_SHIFT;
            traceCounter.putIfAbsent(tracingHeader, 0);
        }
    }

    private Message generateHello() {
        byte[] data = new byte[traceCounter.size() * 2];
        int i = 0;
        Map<Integer, Integer> copy = new HashMap<>(traceCounter);
        for (Map.Entry<Integer, Integer> entry : copy.entrySet()) {
            int tracingHeader = entry.getKey();
            data[i++] = (byte) (tracingHeader >>> MessageHeader.ADDRESS_SHIFT);
            data[i++] = (byte) (tracingHeader >>> MessageHeader.COUNTER_SHIFT);
            int counter = entry.getValue() + 1;
            if (counter >= TRACING_PERIOD) traceCounter.remove(tracingHeader);
            else traceCounter.put(tracingHeader, counter);
        }
        return hello.packAndIncrement(MessageType.Hello, data);
    }

    private Message generateNetworkData() {
        var retx = retxRegister.calculateRetxMap(0.5, RetxRegisterImpl.HISTORY_BREAKPOINT_OPTION);
        var entries = retx.entrySet();
        ByteBuffer buffer = ByteBuffer.allocate(entries.size() * 2);
        entries.forEach(e -> {
            buffer.put(e.getKey());
            buffer.put((byte) (e.getValue() * 256));
        });
        byte[] data = buffer.array();
        return uplink.packAndIncrement(MessageType.UpwardsRouting, data);
    }

    public void feedData(byte... data) {
        send(uplink.packAndIncrement(MessageType.Data, data));
    }

    public byte getNodeId() {
        return nodeId;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public ChannelInfo getMeshChannel() {
        return meshChannel;
    }

    public Set<Byte> getRoutingRegistry() {
        return routingRegistry;
    }

    public Map<Byte, Double> calculateReception() {
        return retxRegister.calculateRetxMap(0.5);
    }

    private void updateRouting(byte[] data) {
        for (byte address : data) {
            if ((address & MessageHeader.DELETE_BIT) != 0) {
                routingRegistry.remove(address);
            } else {
                routingRegistry.add(address);
            }
        }
    }

    private void interpretCommand(String command) {
        String[] parts = command.split(" ");
        byte targetId = Byte.parseByte(parts[0]);
        switch (parts[1]) {
            case "invite" -> {
                byte assignedId = Byte.parseByte(parts[2]);
                byte[] data = new byte[parts[3].length() + 1];
                int i = 0;
                data[i++] = assignedId;
                for (char c : parts[3].toCharArray()) {
                    data[i++] = (byte) c;
                }
                Message message = pceClient.correspondence(targetId).pack(MessageType.DownwardsJoin, data);

                if (targetId == this.nodeId) {

                    handler.when(TRANSMITTER_COOLDOWN_EVENT_KEY)
                            .then(() -> send(message))
                            .labelled(command)
                            .invalidateAfter(JOIN_VOLLEY_AMOUNT);
                } else {
                    handler.immediate(command)
                            .then(() -> send(message));
                }
            }
            case "trace" -> {
                byte[] data = new byte[parts.length - 2];
                for (int i = 1; i < data.length; i++) data[i] = Byte.parseByte(parts[i+2]);
                handler.immediate(command)
                        .then(() -> send(pceClient.correspondence(targetId).pack(MessageType.Trace, data)))
                        .labelled(command);
            }
            case "update" -> {
                byte[] data = new byte[parts.length - 2];
                for (int i = 1; i < data.length; i++) data[i] = Byte.parseByte(parts[i+2]);
                if (targetId == this.nodeId) {
                    updateRouting(data);
                } else {
                    handler.immediate(command)
                            .then(() -> send(pceClient.correspondence(targetId).packAndIncrement(MessageType.DownwardsRouting, data)))
                            .labelled(command);
                }
            }
            default -> throw new IllegalArgumentException("command not interpretable: " + command);
        }

    }

}

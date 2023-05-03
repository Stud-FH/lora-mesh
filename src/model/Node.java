package model;

import model.execution.*;
import model.message.Message;
import model.message.MessageHeader;
import model.message.MessageType;
import util.Randomized;

import java.util.*;

public class Node {

    public static final int HELLO_INTERVAL = 20;
    public static final int SEEKING_INTERVAL = 3 * HELLO_INTERVAL;
    public static final int ROUTING_INTERVAL = 200;
    public static final int TRACING_PERIOD = 10;
    public static final int JOIN_VOLLEY_AMOUNT = 10;
    public static final int INVITE_VOLLEY_AMOUNT = 5;

    private static final String TRANSMITTER_COOLDOWN_EVENT_KEY = "tc";

    private byte id = -1;
    private NodeStatus status = NodeStatus.Down;
    private final Set<Byte> routingRegistry = new HashSet<>();

    private HelloCounter helloCounter;
    private final Map<String, Integer> joinCounter = new HashMap<>();
    private String joinCode = "";
    private final Map<Integer, Integer> traceCounter = new HashMap<>();

    private CorrespondenceManager toController;
    private CorrespondenceManager toNeighbours;

    private final MessageCache cache = new MessageCache(64);
    private final LinkedList<Message> inbox = new LinkedList<>();

    private final EventHandler handler = new EventHandler(this);
    private final NodeConfig config;
    private final TransmitterAdapter transmitter;
    private final ApiAdapter api;
    private final Logger logger;

    public Node(NodeConfig config, TransmitterAdapter transmitter, ApiAdapter api, Logger logger) {
        this.config = config;
        this.transmitter = transmitter;
        this.api = api;
        this.logger = logger;
    }

    public boolean isAlive() {
        return status != NodeStatus.Down && status != NodeStatus.Error;
    }

    public void statusCheck() {
        switch (status) {
            case Controller -> {
                if (!api.testConnection()) {
                    error("disconnected");
                }
            }
            case Node, Joining, Seeking -> {
                if (api.testConnection()) {
                    error("connected");
                }
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
                .then(this::wakeUp);
    }

    public void wakeUp() {
        if (isAlive()) {
            warn("wake up called on live node");
            return;
        } else {
            debug("wake up");
        }

        handler.scheduleWithCooldown(TRANSMITTER_COOLDOWN_EVENT_KEY, transmitter::getSendingIntervalMillis);

        handler.when(e -> e instanceof MessageReceivedEvent)
                        .then(e -> {
                            while (true) {
                                Message message;
                                synchronized (inbox) {
                                    if (inbox.isEmpty()) break;
                                    message = inbox.pop();
                                }
                                switch(status) {
                                    case Controller -> handleMessageAsController(message);
                                    case Node -> handleMessageAsNode(message);
                                    case Joining -> handleMessageAsJoining(message);
                                    case Seeking -> handleMessageAsSeeking(message);
                                }
                            }
                        })
                .labelled("receive");

        transmitter.setReceiveCallback(m -> {
            synchronized (inbox) {
                inbox.push(m);
            }
            handler.fire(new MessageReceivedEvent(m));
        });

        if (api.testConnection()) {
            transmitter.tune(api.getTuning());
            init(api.nextId(), true);
        } else if (config.transmitterTuning() != null) {
            transmitter.tune(config.transmitterTuning());
            join();
        }
        else {
            seek();
        }
    }

    public void refresh() {
//        debug("notifying handler");
        synchronized (handler) {
            handler.notifyAll();
        }
    }

    private boolean shouldForward(Message message) {
        return routingRegistry.contains(message.getRoutingAddress())
                && (MessageType.Resend.matches(message) || !cache.contains(message));
    }

    private void send(Message message) {
        statusCheck();
        if (MessageType.Hello.matches(message)
                || MessageType.Trace.matches(message)
                || MessageType.Resend.matches(message)) {
            debug("sending uncached %s", message);
            transmitter.send(message);
        } else if (MessageType.Upwards.matches(message) && status == NodeStatus.Controller) {
            debug("to api: %s", message);
            var commands = api.receive(this.id, message);
            debug("api answered: %s", commands);
            for (var command : commands) interpretCommand(command);
        } else {
            info("sending %s", message);
            cache.store(message);
            transmitter.send(message);
        }
    }

    private void seek() {
        debug("entering seek mode...");
        id = -1;
        status = NodeStatus.Seeking;
        handler.when(TRANSMITTER_COOLDOWN_EVENT_KEY)
                .then(e -> transmitter.tuneStep())
                .labelled("tune")
                .reactEvery(SEEKING_INTERVAL)
                .invalidateIf(() -> id != -1);
        debug("seeking...");
    }

    private void handleMessageAsSeeking(Message message) {
        debug("received message: %s", message);
        join();
    }

    private void join() {
        debug("entering join mode...");
        id = 0;
        status = NodeStatus.Joining;

        joinCode = config.joinCode() != null? config.joinCode() : Randomized.string(4);
        debug("join code: %s", joinCode);
        byte[] joinBytes = new byte[joinCode.length() + 1];
        int i = 1;
        for (char c : joinCode.toCharArray()) joinBytes[i++] = (byte) c;

        toNeighbours = LocalCorrespondenceManager.from(id);

        handler.when(TRANSMITTER_COOLDOWN_EVENT_KEY)
                .then(() -> send(toNeighbours.packAndIncrement(MessageType.Hello, joinBytes)))
                .labelled("send join request")
                .invalidateAfter(JOIN_VOLLEY_AMOUNT);

        debug("joining...");
    }

    private void handleMessageAsJoining(Message message) {
        debug("received message: %s", message);
        if (MessageType.DownwardsJoin.matches(message) && message.hasData()) {
            byte assignedId = message.data(0);
            if (message.dataString(1).equals(joinCode)) {
                init(assignedId, false);
            }
        }
    }

    private void init(byte nodeId, boolean controller) {
        info("init as %s %d", controller? "controller" : "node", nodeId);
        id = nodeId;
        toNeighbours = LocalCorrespondenceManager.from(id);
        toController = LocalCorrespondenceManager.from(id);
        helloCounter = new HelloCounter();
        status = controller? NodeStatus.Controller : NodeStatus.Node;

        handler.when(TRANSMITTER_COOLDOWN_EVENT_KEY)
                .then(() -> send(generateHello()))
                .labelled("hello")
                .reactEvery(HELLO_INTERVAL);

        handler.when(TRANSMITTER_COOLDOWN_EVENT_KEY)
                .then(() -> send(generateNetworkData()))
                .labelled("routing")
                .reactEvery(ROUTING_INTERVAL);
    }

    private void handleMessageAsController(Message message) {
        if (MessageType.Hello.matches(message)) {
            handleHello(message);
        } else if (MessageType.Trace.matches(message)) {
            handleTrace(message);
        } else {
            debug("to api: %s", message);
            var commands = api.receive(this.id, message);
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
//        debug("received hello: %s", message);
        if (message.getNodeId() == id) {
            warn("received own hello");
        } else if (message.getNodeId() == 0) {
            var code = message.dataString(1);


            joinCounter.compute(code, (k, v) -> {

                if (v == null) {
                    handler.delayed("join " + code, () -> transmitter.getSendingIntervalMillis() * JOIN_VOLLEY_AMOUNT * 2)
                            .then(() -> {
                                byte reliability = (byte) (255f * joinCounter.remove(code) / JOIN_VOLLEY_AMOUNT);
                                byte[] data = message.data();
                                data[0] = reliability;
                                send(toController.packAndIncrement(MessageType.UpwardsJoin, data));
                            });
                    return 1;
                } else {
                    return v + 1;
                }
            });
        } else {
            helloCounter.count(message);

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

        if (!uncached.isEmpty() && message.getNodeId() != id) {
            byte[] data = new byte[uncached.size()];
            int i = 0;
            for (byte b : uncached) data[i++] = b;
            handleDefaultAsNode(new Message(message.header(), data));
        }
    }

    private void handleJoin(Message message) {
        debug("received join: %s", message);
        if (MessageType.Downwards.matches(message) && message.getNodeId() == id) {
            registerMessage(message);
            byte assignedId = message.data(0);
            routingRegistry.add(assignedId);
            routingRegistry.add((byte) (assignedId | MessageHeader.DOWNWARDS_BIT));

            handler.when(TRANSMITTER_COOLDOWN_EVENT_KEY)
                    .then(() -> send(message))
                    .labelled("invite " + assignedId)
                    .reactEvery(HELLO_INTERVAL + 1)
                    .invalidateIf(() -> helloCounter.knows(assignedId));

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
        if (MessageType.Downwards.matches(message) && message.getNodeId() == id) {
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
        var lost = toController.registerAndListLosses(message);
        for (byte b : lost) {
            int tracingHeader = 0;
            tracingHeader |= MessageHeader.DOWNWARDS_BIT;
            tracingHeader |= id << MessageHeader.ADDRESS_SHIFT;
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
        return toNeighbours.packAndIncrement(MessageType.Hello, data);
    }

    private Message generateNetworkData() {
        helloCounter.calculateReception(true);
        return toController.packAndIncrement(MessageType.UpwardsRouting, NodeSnapshot.receptionData(this));
    }

    public void feedData(byte... data) {
        send(toController.packAndIncrement(MessageType.Data, data));
    }

    public byte getId() {
        return id;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public String getTuning() {
        return transmitter.getTuning();
    }

    public Set<Byte> getRoutingRegistry() {
        return routingRegistry;
    }

    public Map<Byte, Byte> calculateReception() {
        return helloCounter == null? null : helloCounter.calculateReception(false);
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
                Message message = api.correspondence(targetId).pack(MessageType.DownwardsJoin, data);

                if (targetId == this.id) {

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
                        .then(() -> send(api.correspondence(targetId).pack(MessageType.Trace, data)))
                        .labelled(command);
            }
            case "update" -> {
                byte[] data = new byte[parts.length - 2];
                for (int i = 1; i < data.length; i++) data[i] = Byte.parseByte(parts[i+2]);
                if (targetId == this.id) {
                    updateRouting(data);
                } else {
                    handler.immediate(command)
                            .then(() -> send(api.correspondence(targetId).packAndIncrement(MessageType.DownwardsRouting, data)))
                            .labelled(command);
                }
            }
            default -> throw new IllegalArgumentException("command not interpretable: " + command);
        }

    }

}

package local;

import model.Module;
import model.*;
import model.Executor;
import model.message.Message;
import model.message.MessageHeader;
import model.message.MessageType;
import model.message.MessageUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;

public class Node implements Module {

    public static final long STATUS_CHECK_PERIOD = 60000;
    public static final long STATUS_CHECK_DELAY = 32000;
    public static final long HELLO_PERIOD = 20000;
    public static final long INVITE_RESPONSE_TIMEOUT = 40100;
    public static final long HELLO_DELAY = 0;
    public static final long RENDEZVOUS_PERIOD = 65000;
    public static final long RENDEZVOUS_DELAY = 27000;
    public static final long ROUTING_PERIOD = 120000;
    public static final long ROUTING_DELAY = 107000;
    public static final int TRACING_VOLLEY = 10;
    public static final int JOIN_VOLLEY = 10;
    public static final long JOIN_DELAY = 10;


    public final long serialId;
    private byte nodeId = -1;
    private NodeStatus status = NodeStatus.Down;
    boolean isController = false;
    private ChannelInfo meshChannel;
    private final Set<Byte> routingRegistry = new HashSet<>();

    private RetxRegister retxRegister;
    private final Map<Integer, Double> retxMap = new HashMap<>();
    private final Map<String, Integer> joinCounter = new HashMap<>();
    private final Map<Integer, Integer> traceCounter = new HashMap<>();

    private CorrespondenceRegister uplink;
    private CorrespondenceRegister hello;

    private final MessageCache cache = new MessageCache(64);
    private boolean dataSinkConnected = false;
    private Logger logger;
    private LoRaMeshClient lora;
    private PceClient pce;
    private DataSinkClient dataSink;
    private Executor exec;
    private CommandLine cmd;
    private Runnable destroyCtx;


    private Consumer<Message> messageHandler;

    public Node(long sid) {
        this.serialId = sid;
    }

    @Override
    public void useContext(ApplicationContext ctx) {
        logger = ctx.resolve(Logger.class);
        exec = ctx.resolve(Executor.class);
        cmd = ctx.resolve(CommandLine.class);
        lora = ctx.resolve(LoRaMeshClient.class);
        pce = ctx.resolve(PceClient.class);
        dataSink = ctx.resolve(DataSinkClient.class);
        destroyCtx = ctx::destroy;
    }

    @Override
    public void deploy() {
        exec.schedule(this::wakeUp, 50);
    }

    @Override
    public void destroy() {
        info("shutting down");
    }

    public boolean isAlive() {
        return status != NodeStatus.Down && status != NodeStatus.Error;
    }

    public void statusCheck() {
        switch (status) {
            case Node:
                var result = pce.heartbeat(snapshot());
                if (result != null) error("connected");
                dataSinkConnected = dataSink.heartbeat();
                break;

            case Controller:
                meshChannel = pce.heartbeat(snapshot());
                if (meshChannel == null) error("disconnected");
                dataSinkConnected = dataSink.heartbeat();
                break;
        }
    }

    public void debug(String format, Object... args) {
        logger.debug(String.format(format, args), this);
    }

    public void info(String format, Object... args) {
        logger.info(String.format(format, args), this);
    }

    public void warn(String format, Object... args) {
        logger.warn(String.format(format, args), this);
    }

    public void error(String format, Object... args) {
        status = NodeStatus.Error;
        logger.error(String.format(format, args), this);
        destroyCtx.run();
        cmd.sync("sudo", "reboot");
    }

    public void wakeUp() {
        if (isAlive()) {
            warn("wake up called on live node");
            return;
        } else {
            debug("launch");
        }

        dataSinkConnected = dataSink.heartbeat();
        meshChannel = pce.heartbeat(snapshot());
        if (meshChannel != null) {
            isController = true;
            initNode(pce.allocateNodeId(serialId, (byte) -1, 0.0), true);
        } else {
            seek();
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
            lora.enqueue(meshChannel, message);
        } else if (MessageType.Data.matches(message) && dataSinkConnected) {
            info("feeding data sink: %s", message);
            dataSink.feed(message);
        } else if (MessageType.Upwards.matches(message) && status == NodeStatus.Controller) {
            debug("feeding pce: %s", message);
            var commands = pce.feed(this.serialId, message);
            debug("pce answered: %s", commands);
            for (var command : commands) interpretCommand(command);
        } else {
            info("sending %s", message);
            cache.store(message);
            lora.enqueue(meshChannel, message);
        }
    }

    private void sendRendezvous() {
        lora.enqueue(ChannelInfo.rendezvous,
                new Message(0, MessageUtil.channelInfoToRendezvousData(meshChannel)));
    }

    private void seek() {
        debug("entering seek mode...");
        nodeId = -1;
        status = NodeStatus.Seeking;

        lora.listen(ChannelInfo.rendezvous, new MessageObserver(m -> {
            meshChannel = MessageUtil.rendezvousDataToChannelInfo(m.data());
            join();
        }, () -> nodeId != -1));
    }

    private void join() {
        debug("entering join mode...");
        nodeId = 0;
        status = NodeStatus.Joining;

        hello = LocalCorrespondenceRegister.from(nodeId);
        var data = MessageUtil.serialIdToJoinData(serialId);

        messageHandler = message -> {
            if (MessageType.DownwardsJoin.matches(message) && message.hasData()) {
                var result = MessageUtil.inviteDataToInviteResult(message.data);
                if (result.serialId == this.serialId) {
                    initNode(result.assignedId, false);
                }
            }
        };

        lora.listen(meshChannel, new MessageObserver(messageHandler, this::isAlive));

        for (int i = 0; i < JOIN_VOLLEY; i++) {
            send(hello.packAndIncrement(MessageType.Hello, data));
        }
    }

    private void initNode(byte assignedId, boolean controller) {
        info("init as %s %d", controller? "controller" : "node", assignedId);
        this.nodeId = assignedId;
        hello = LocalCorrespondenceRegister.from(this.nodeId);
        uplink = LocalCorrespondenceRegister.from(this.nodeId);
        retxRegister = new RetxRegisterImpl();
        status = controller? NodeStatus.Controller : NodeStatus.Node;

        exec.schedulePeriodic(this::statusCheck, STATUS_CHECK_PERIOD, STATUS_CHECK_DELAY);
        exec.schedulePeriodic(() -> send(generateHello()), HELLO_PERIOD, HELLO_DELAY);
        exec.schedulePeriodic(this::sendRendezvous, RENDEZVOUS_PERIOD, RENDEZVOUS_DELAY);
        exec.schedulePeriodic(() -> send(generateNetworkData()), ROUTING_PERIOD, ROUTING_DELAY);

        messageHandler = controller? this::handleMessageAsController : this::handleMessageAsNode;
    }

    private void  handleMessageAsController(Message message) {
        if (MessageType.Hello.matches(message)) {
            handleHello(message);
        } else if (MessageType.Trace.matches(message)) {
            handleTrace(message);
        } else {
            debug("to api: %s", message);
            var commands = pce.feed(this.serialId, message);
            debug("api answered: %s", commands);
            for (var command : commands) interpretCommand(command);
        }
    }

    private void handleMessageAsNode(Message message) {
        switch (MessageType.pure(message)) {
            case Hello: handleHello(message); break;
            case Join: handleJoin(message); break;
            case Routing: handleRouting(message); break;
            case Trace: handleTrace(message); break;
            default: handleDefaultAsNode(message); break;
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
                    exec.schedule(() -> {
                                byte reliability = (byte) (255f * joinCounter.remove(code) / JOIN_VOLLEY);
                                byte[] data = message.data();
                                data[0] = reliability;
                                send(uplink.packAndIncrement(MessageType.UpwardsJoin, data));
                            }, JOIN_DELAY);
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
                        exec.async(() -> send(restored));
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

            exec.async(() -> this.invite(assignedId, message));

        } else if (MessageType.Downwards.matches(message) && shouldForward(message)) {
            routingRegistry.add(message.data(0));
            routingRegistry.add((byte) (message.data(0) | MessageHeader.DOWNWARDS_BIT));
            send(message);
        } else {
            handleDefaultAsNode(message);
        }
    }

    private void invite(byte assignedId, Message message) {
        if (!retxRegister.knows(assignedId)) {
            send(message);
            exec.schedule(() -> this.invite(assignedId, message), INVITE_RESPONSE_TIMEOUT);
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
            if (counter >= TRACING_VOLLEY) traceCounter.remove(tracingHeader);
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
        if (uplink == null) {
            warn("data loss in status %s: %s", status, Arrays.toString(data));
            return;
        }
        try {
            send(uplink.packAndIncrement(MessageType.Data, data));
        } catch (Exception e) {
            logger.exception(e, this);
        }
    }

    public byte getNodeId() {
        return nodeId;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public Set<Byte> getRoutingRegistry() {
        return routingRegistry;
    }

    public NodeInfo snapshot() {
        return new NodeInfo(serialId, status, nodeId, retxMap);
    }

    @Override
    public String info() {
        return String.format("Node %d (%s/%d)", serialId, status, nodeId);
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
            case "invite": {
                byte assignedId = Byte.parseByte(parts[2]);
                byte[] data = new byte[parts[3].length() + 1];
                int i = 0;
                data[i++] = assignedId;
                for (char c : parts[3].toCharArray()) {
                    data[i++] = (byte) c;
                }
                Message message = pce.correspondence(targetId).pack(MessageType.DownwardsJoin, data);

                if (targetId == this.nodeId) {
                    for (int j = 0; j < JOIN_VOLLEY; j++) {
                        send(message);
                    }
                } else {
                    exec.async(() -> send(message));
                }
            }
                break;
            case "trace": {
                byte[] data = new byte[parts.length - 2];
                for (int i = 1; i < data.length; i++) data[i] = Byte.parseByte(parts[i + 2]);
                exec.async(() -> send(pce.correspondence(targetId).pack(MessageType.Trace, data)));
            }
                break;
            case "update": {
                byte[] data = new byte[parts.length - 2];
                for (int i = 1; i < data.length; i++) data[i] = Byte.parseByte(parts[i + 2]);
                if (targetId == this.nodeId) {
                    updateRouting(data);
                } else {
                    exec.async(() -> send(pce.correspondence(targetId).packAndIncrement(MessageType.DownwardsRouting, data)));
                }
            }
                break;
            default: throw new IllegalArgumentException("command not interpretable: " + command);
        }

    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Logger.class, CommandLine.class, DataSinkClient.class, PceClient.class, LoRaMeshClient.class);
    }

    @Override
    public Collection<Class<? extends Module>> providers() {
        return Set.of(Node.class);
    }
}

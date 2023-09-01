package v2.core.domain.node;

import v2.core.common.BasicSubject;
import v2.core.common.Subject;
import v2.core.concurrency.Executor;
import v2.core.concurrency.CancellationToken;
import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.domain.*;
import v2.core.domain.message.Message;
import v2.core.domain.message.MessageHeader;
import v2.core.domain.message.MessageType;
import v2.core.log.Logger;
import v2.core.util.MessageUtil;
import v2.shared.impl.LocalCorrespondenceRegister;
import v2.shared.impl.RetxRegisterImpl;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;

public class Node implements Module {

    public static final boolean DO_TRACE = false;

    public static final long STATUS_CHECK_PERIOD = 60000;
    public static final long STATUS_CHECK_DELAY = 32000;
    public static final long HELLO_PERIOD = 20000;
    public static final long INVITE_RESPONSE_TIMEOUT = 40100;
    public static final long HELLO_DELAY = 0;
    public static final long RENDEZVOUS_PERIOD = 65000;
    public static final long RENDEZVOUS_DELAY = 0;
    public static final long ROUTING_PERIOD = 120000;
    public static final long ROUTING_DELAY = 107000;
    public static final int TRACING_VOLLEY = 10;
    public static final int JOIN_VOLLEY = 10;
    public static final long JOIN_DELAY = 1000;
    public static final long JOIN_TIMEOUT = 3000;

    private int address = -1;
    private final BasicSubject<NodeStatus> status = new BasicSubject<>(NodeStatus.Down);
    private ChannelInfo meshChannel;
    private final Set<Integer> routingRegistry = new HashSet<>();

    private RetxRegister retxRegister;
    private final Map<Long, Integer> joinCounter = new HashMap<>();
    private final Map<Integer, Integer> traceCounter = new HashMap<>();

    private List<CancellationToken> cancellationTokens = new ArrayList<>();

    private CorrespondenceRegister uplink;
    private CorrespondenceRegister hello;

    private final MessageCache cache = new MessageCache(64);
    private boolean dataSinkConnected = false;
    private Config config;
    private OsAdapter os;
    private Executor exec;
    private Logger logger;
    private LoRaMeshModule lora;
    private PceModule pce;
    private DataSinkModule dataSink;
    private Consumer<String> teardownCallback;

    @Override
    public void build(Context ctx) {
        config = ctx.resolve(Config.class);
        os = ctx.resolve(OsAdapter.class);
        logger = ctx.resolve(Logger.class);
        exec = ctx.resolve(Executor.class);
        lora = ctx.resolve(LoRaMeshModule.class);
        pce = ctx.resolve(PceModule.class);
        dataSink = ctx.resolve(DataSinkModule.class);
        teardownCallback = ctx::destroy;
    }

    @Override
    public void deploy() {
        var ct = exec.schedule(this::wakeUp, 50);
        cancellationTokens.add(ct);
    }

    @Override
    public void destroy() {
        info("shutting down");
        cancelAllProcedures();
    }

    private synchronized void cancelAllProcedures() {
        var cancelling = cancellationTokens;
        cancellationTokens = new ArrayList<>();
        cancelling.forEach(CancellationToken::cancel);
    }

    public long id() {
        return config.id();
    }

    public int address() {
        return address;
    }

    public Subject<NodeStatus> status() {
        return status;
    }

    public Map<Integer, Double> retx() {
        return retxRegister== null? new HashMap<>() : retxRegister.calculateRetx(0.2);
    }

    public boolean isAlive() {
        return status.get() != NodeStatus.Down && status.get() != NodeStatus.Error;
    }

    public void statusCheck() {
        debug("status check");
        switch (status.get()) {
            case Node:
                var result = pce.heartbeat();
                if (result != null) error("connected");
                dataSinkConnected = dataSink.heartbeat();
                break;

            case Controller:
                meshChannel = pce.heartbeat();
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

    public void error(String message) {
        status.set(NodeStatus.Error);
        logger.error(message, this);
        teardownCallback.accept(String.format("fatal error on node %d: %s", id(), message));
        os.reboot();
    }

    public void wakeUp() {
        if (isAlive()) {
            warn("wake up called on live node");
            return;
        } else {
            debug("launch");
        }

        dataSinkConnected = dataSink.heartbeat();
        meshChannel = pce.heartbeat();
        if (meshChannel != null) {
            initNode(pce.allocateAddress(id(), (byte) -1, 0.0), true);
        } else {
            seek();
        }
    }

    private boolean shouldForward(Message message) {
        return routingRegistry.contains(message.getRoutingAddress())
                && (MessageType.Resend.matches(message) || !cache.contains(message));
    }

    private void emit(Message message) {
        if (MessageType.Hello.matches(message)
                || MessageType.Trace.matches(message)
                || MessageType.Resend.matches(message)) {
            debug("sending uncached %s", message);
            lora.enqueue(meshChannel, message);
        } else if (MessageType.Data.matches(message) && dataSinkConnected) {
            info("feeding data sink: %s", message);
            var tracingHeaders = dataSink.feed(message);
            registerTracingHeaders(tracingHeaders);
        } else if (MessageType.Upwards.matches(message) && status.get() == NodeStatus.Controller) {
            debug("feeding pce: %s", message);
            var commands = pce.feed(id(), message);
            if (commands.isEmpty()) {
                debug("up to date");
            }
            for (var command : commands) interpretCommand(command);
        } else {
            info("sending %s", message);
            cache.store(message);
            lora.enqueue(meshChannel, message);
        }
    }

    private void sendRendezvous() {
        debug("rendezvous emit");
        lora.enqueue(ChannelInfo.rendezvous,
                new Message(0, MessageUtil.channelInfoToRendezvousData(meshChannel)));
    }

    private void seek() {
        debug("entering seek mode...");
        address = -1;
        status.set(NodeStatus.Seeking);

        lora.listen(ChannelInfo.rendezvous, m -> {
            meshChannel = MessageUtil.rendezvousDataToChannelInfo(m.data());
            join();
        });
    }

    private void join() {
        debug("entering join mode...");

        if (meshChannel == null) {
            error("mesh channel not set");
        }

        address = 0;
        status.set(NodeStatus.Joining);

        hello = LocalCorrespondenceRegister.from(address);
        var data = MessageUtil.sidToJoinData(id());

        lora.listen(meshChannel, message -> {
            if (MessageType.DownwardsJoin.matches(message) && message.hasData()) {
                var result = MessageUtil.inviteDataToInviteResult(message.data);
                if (result.serialId == id()) {
                    initNode(result.assignedId, false);
                }
            }
        });

        for (int i = 0; i < JOIN_VOLLEY; i++) {
            emit(hello.packAndIncrement(MessageType.Hello, data));
        }

        var ct = exec.schedule(this::join, JOIN_TIMEOUT); // timeout will be cancelled when joined successfully
        cancellationTokens.add(ct);
    }

    private void initNode(int assignedAddress, boolean controller) {
        info("init as %s %d", controller? "controller" : "node", assignedAddress);
        cancelAllProcedures();
        this.address = assignedAddress;
        hello = LocalCorrespondenceRegister.from(this.address);
        uplink = LocalCorrespondenceRegister.from(this.address);
        retxRegister = new RetxRegisterImpl();
        status.set(controller? NodeStatus.Controller : NodeStatus.Node);

        var ct = exec.schedulePeriodic(this::sendRendezvous, RENDEZVOUS_PERIOD, RENDEZVOUS_DELAY);
        cancellationTokens.add(ct);
        ct = exec.schedulePeriodic(this::statusCheck, STATUS_CHECK_PERIOD, STATUS_CHECK_DELAY);
        cancellationTokens.add(ct);
        ct = exec.schedulePeriodic(() -> emit(generateHello()), HELLO_PERIOD, HELLO_DELAY);
        cancellationTokens.add(ct);
        ct = exec.schedulePeriodic(() -> emit(generateNetworkData()), ROUTING_PERIOD, ROUTING_DELAY);
        cancellationTokens.add(ct);

        lora.listen(meshChannel, controller? this::handleMessageAsController : this::handleMessageAsNode);
    }

    private void  handleMessageAsController(Message message) {
        if (MessageType.Hello.matches(message)) {
            handleHello(message);
        } else if (MessageType.Trace.matches(message)) {
            handleTrace(message);
        } else {
            debug("feeding api: %s", message);
            var commands = pce.feed(id(), message);
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
        if (message.getAddress() == address) {
            warn("received own hello");
        } else if (message.getAddress() == 0) {
            ByteBuffer buf = ByteBuffer.wrap(message.data());
            if (buf.remaining() < 9) {
                warn("short join: %s", message);
                return;
            }
            buf.get(); // left empty
            long id = buf.getLong();

            joinCounter.compute(id, (k, v) -> {
                if (v == null) {
                    var ct = exec.schedule(() -> {
                                byte reliability = (byte) (255f * joinCounter.remove(id) / JOIN_VOLLEY);
                                ByteBuffer buf2 = ByteBuffer.allocate(9);
                                buf2.put(reliability);
                                buf2.putLong(id);
                                emit(uplink.packAndIncrement(MessageType.UpwardsJoin, buf2.array()));
                            }, JOIN_DELAY);
                    cancellationTokens.add(ct);
                    return 1;
                } else {
                    return v + 1;
                }
            });
        } else {
            retxRegister.next(message);

            registerTracingHeaders(MessageUtil.helloDataToTracingHeaders(message.data()));
        }
    }

    private void handleTrace(Message message) {
        if (!DO_TRACE) {
            return;
        }
        debug("received trace: %s", message);

        Collection<Byte> uncached = new ArrayList<>();
        for (byte correspondenceCounter : message.data()) {
            Message restored = cache.restore(message.getTracingHeader(correspondenceCounter));
            if (restored != null) {
                emit(restored);
            } else {
                uncached.add(correspondenceCounter);
            }
        }

        if (!uncached.isEmpty() && message.getAddress() != address) {
            byte[] data = new byte[uncached.size()];
            int i = 0;
            for (byte b : uncached) data[i++] = b;
            handleDefaultAsNode(new Message(message.header(), data));
        }
    }

    private void handleJoin(Message message) {
        debug("received join: %s", message);
        int address = message.data(0);
        if (MessageType.Downwards.matches(message) && message.getAddress() == address) {
            registerTracingHeaders(MessageUtil.countersToTracingHeaders(uplink.address(), uplink.registerAndListLosses(message)));
            routingRegistry.add(address);
            routingRegistry.add(address | MessageHeader.DOWNWARDS_BIT);

            var ct = exec.async(() -> this.invite(address, message));
            cancellationTokens.add(ct);

        } else if (MessageType.Downwards.matches(message) && shouldForward(message)) {
            routingRegistry.add(address);
            routingRegistry.add(message.data(0) | MessageHeader.DOWNWARDS_BIT);
            emit(message);
        } else {
            handleDefaultAsNode(message);
        }
    }

    private void invite(int address, Message message) {
        if (!retxRegister.knows(address)) {
            emit(message);
            var ct = exec.schedule(() -> this.invite(address, message), INVITE_RESPONSE_TIMEOUT);
            cancellationTokens.add(ct);
        }
    }
    private void handleRouting(Message message) {
        debug("received routing: %s", message);
        if (MessageType.Downwards.matches(message) && message.getAddress() == address) {
            registerTracingHeaders(MessageUtil.countersToTracingHeaders(uplink.address(), uplink.registerAndListLosses(message)));
            updateRouting(message.data());
        } else {
            handleDefaultAsNode(message);
        }
    }

    private void handleDefaultAsNode(Message message) {
        debug("received message: %s", message);
        cache.store(message);
        if (shouldForward(message)) {
            emit(message);
        }
    }

    private synchronized Message generateHello() {
        if (!DO_TRACE) {
            return hello.packAndIncrement(MessageType.Hello);
        }
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
        var retx = retx();
        ByteBuffer buffer = ByteBuffer.allocate(retx.size() * 2);
        retx.forEach((key, value) -> {
            buffer.put(key.byteValue());
            buffer.put((byte) (value * 256));
        });
        byte[] data = buffer.array();
        return uplink.packAndIncrement(MessageType.UpwardsRouting, data);
    }

    public void feedData(byte... data) {
        if (uplink == null) {
//            warn("data loss in status %s: %s", status, Arrays.toString(data));
            return;
        }
        try {
            emit(uplink.packAndIncrement(MessageType.Data, data));
        } catch (Exception e) {
            logger.exception(e, this);
        }
    }

    public Set<Integer> getRoutingRegistry() {
        return routingRegistry;
    }

    @Override
    public String info() {
        return String.format("Node %d (%s/%d)", id(), status, address);
    }

    private void updateRouting(byte[] data) {
        for (int address : data) {
            if ((address & MessageHeader.DELETE_BIT) != 0) {
                routingRegistry.remove(address & ~MessageHeader.DELETE_BIT);
            } else {
                routingRegistry.add(address);
            }
        }
    }

    private void registerTracingHeaders(Collection<Integer> tracingHeaders) {
        if (!DO_TRACE) {
            return;
        }
        for (int tracingHeader : tracingHeaders) {
            if (MessageType.Resolved.matches(tracingHeader)) {
                traceCounter.putIfAbsent(tracingHeader, 0);
                traceCounter.remove(tracingHeader & ~MessageHeader.RESOLVED_BIT);
            } else {
                Message restored = cache.restore(tracingHeader);
                if (restored != null) {
                    traceCounter.remove(tracingHeader);
                    traceCounter.putIfAbsent(tracingHeader & ~MessageHeader.RESOLVED_BIT, 0);
                    var ct = exec.async(() -> emit(restored));
                    cancellationTokens.add(ct);
                } else if ((tracingHeader & MessageHeader.ADDRESS_MASK) != address) {
                    traceCounter.putIfAbsent(tracingHeader, 0);
                }
            }
        }
    }

    private void interpretCommand(String command) {
        debug("command: %s", command);
        String[] parts = command.split(" ");
        byte targetId = Byte.parseByte(parts[0]);
        switch (parts[1]) {
            case "invite": {
                byte address = Byte.parseByte(parts[2]);
                long id = Long.parseLong(parts[3]);
                ByteBuffer buf = ByteBuffer.allocate(9);
                buf.put(address);
                buf.putLong(id);
                byte[] data = buf.array();
                Message message = pce.correspondence(targetId).pack(MessageType.DownwardsJoin, data);

                if (targetId == this.address) {
                    for (int j = 0; j < JOIN_VOLLEY; j++) {
                        emit(message);
                    }
                } else {
                    var ct = exec.async(() -> emit(message));
                    cancellationTokens.add(ct);
                }
            }
                break;
            case "trace": {
                if (DO_TRACE) {
                    byte[] data = new byte[parts.length - 2];
                    for (int i = 2; i < parts.length; i++) data[i - 2] = (byte) Integer.parseInt(parts[i]);
                    var ct = exec.async(() -> emit(pce.correspondence(targetId).pack(MessageType.Trace, data)));
                    cancellationTokens.add(ct);
                }
            }
                break;
            case "update": {
                byte[] data = new byte[parts.length - 2];
                for (int i = 2; i < parts.length; i++) data[i - 2] = (byte) Integer.parseInt(parts[i]);
                if (targetId == this.address) {
                    updateRouting(data);
                } else {
                    var ct = exec.async(() -> emit(pce.correspondence(targetId).packAndIncrement(MessageType.DownwardsRouting, data)));
                    cancellationTokens.add(ct);
                }
            }
                break;
            default: throw new IllegalArgumentException("command not interpretable: " + command);
        }

    }

    public interface Config extends Module {
        long id();
    }
}

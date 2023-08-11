package v2.simulation.impl;

import v2.core.concurrency.Executor;
import v2.core.log.Logger;
import v2.core.domain.ChannelInfo;
import v2.core.domain.LoRaMeshClient;
import v2.core.domain.Observer;
import v2.core.context.Context;
import v2.core.domain.message.Message;
import v2.core.domain.message.MessageType;
import v2.simulation.Simulation;
import v2.simulation.util.NodeHandle;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

public class SimulatedLoRaMeshClient implements LoRaMeshClient, Serializable {

    private Simulation simulation;
    private Executor exec;
    private NodeHandle handle;
    private Logger logger;
    private Consumer<Message> receiveCallback;
    private final Queue<Item> queue = new LinkedList<>();
    private ChannelInfo listeningChannel;
    private long lastSent;
    private long lastHello;

    @Override
    public void build(Context ctx) {
        simulation = ctx.resolve(Simulation.class);
        exec = ctx.resolve(Executor.class);
        handle = ctx.resolve(NodeHandle.class);
        logger = ctx.resolve(Logger.class);
    }

    @Override
    public void deploy() {
        exec.schedulePeriodic(this::trigger, 1000, 500);
    }

    private void trigger() {
        if (queue.isEmpty()) {
            return;
        }
        var item = queue.poll();
        var message = item.message;
        var channel = item.channel;
        if (message.dataLength() > 12) logger.warn("sending long message: " + message, this);
        if (MessageType.Hello.matches(message)) {
            lastHello = System.currentTimeMillis();
        } else {
            lastSent = System.currentTimeMillis();
        }

        Random r = new Random();
        simulation.all().stream()
                .filter(NodeHandle::isAlive)
                .filter(n -> n.listeningChannel().equals(channel))
                .filter(n -> n != handle)
                .filter(n -> n.reception(handle) >= r.nextDouble())
                .forEach(other -> other.receive(message));

    }

    @Override
    public synchronized void enqueue(ChannelInfo channel, Message message) {
        queue.add(new Item(message, channel));
    }

    public void receive(Message message) {
        receiveCallback.accept(message);
    }

    @Override
    public void listen(ChannelInfo channelInfo, Observer<Message> observer) {
        this.listeningChannel = channelInfo;
        this.receiveCallback = observer::next;
    }

    public ChannelInfo listeningChannel() {
        return listeningChannel;
    }

    public long lastSent() {
        return lastSent;
    }

    public long lastHello() {
        return lastHello;
    }

    private static class Item {
        final Message message;
        final ChannelInfo channel;

        public Item(Message message, ChannelInfo channel) {
            this.message = message;
            this.channel = channel;
        }
    }
}

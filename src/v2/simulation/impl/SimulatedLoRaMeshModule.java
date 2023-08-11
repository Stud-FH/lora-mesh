package v2.simulation.impl;

import v2.core.common.Observable;
import v2.core.concurrency.Executor;
import v2.core.context.Context;
import v2.core.domain.ChannelInfo;
import v2.core.domain.LoRaMeshModule;
import v2.core.common.Observer;
import v2.core.domain.message.Message;
import v2.core.domain.message.MessageType;
import v2.core.log.Logger;
import v2.shared.measurements.LoraMeshModuleInsights;
import v2.simulation.Simulation;
import v2.simulation.util.NodeHandle;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class SimulatedLoRaMeshModule implements LoRaMeshModule, LoraMeshModuleInsights, Serializable {

    private Simulation simulation;
    private Executor exec;
    private NodeHandle handle;
    private Logger logger;
    private final Observable<Message> triggered = new Observable<>();
    private final Observable<Message> received = new Observable<>();
    private final Queue<Item> queue = new LinkedList<>();
    private ChannelInfo listeningChannel;
    private Observer.Ref listeningObserverRef;
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

    @Override
    public Observable<Message> triggered() {
        return triggered;
    }

    private void trigger() {
        if (queue.isEmpty()) {
            triggered.next(null);
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

        triggered.next(message);

        Random r = new Random();
        simulation.all().stream()
                .filter(NodeHandle::isAlive)
                .filter(n -> channel.equals(n.listeningChannel()))
                .filter(n -> n != handle)
                .filter(n -> n.reception(handle) >= r.nextDouble())
                .forEach(other -> other.receive(message));
    }

    @Override
    public synchronized void enqueue(ChannelInfo channel, Message message) {
        queue.add(new Item(message, channel));
    }

    public void receive(Message message) {
        received.next(message);
    }

    @Override
    public void listen(ChannelInfo channelInfo, Observer<Message> observer) {
        if (this.listeningObserverRef != null) {
            listeningObserverRef.unsubscribe();
        }
        this.listeningChannel = channelInfo;
        listeningObserverRef = received.subscribe(observer);
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

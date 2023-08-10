package v2.simulation;

import v2.core.log.Logger;
import v2.core.domain.ChannelInfo;
import v2.core.domain.LoRaMeshClient;
import v2.core.domain.Observer;
import v2.core.context.Context;
import v2.core.domain.message.Message;
import v2.core.domain.message.MessageType;
import v2.simulation.util.NodeLabel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class SimulatedLoRaMeshClient implements LoRaMeshClient, Serializable {

    private Simulation simulation;
    private NodeHandle handle;
    private NodeLabel label;
    private Logger logger;
    private Consumer<Message> receiveCallback;
    ChannelInfo listeningChannel;
    long lastSent;
    long lastHello;

    @Override
    public void build(Context ctx) {
        simulation = ctx.resolve(Simulation.class);
        handle = ctx.resolve(NodeHandle.class);
        label = ctx.resolve(NodeLabel.class);
        logger = ctx.resolve(Logger.class);
    }

    @Override
    public void enqueue(ChannelInfo channel, Message message) {
        if (message.dataLength() > 12) logger.warn("sending long message: " + message, this);
        if (MessageType.Hello.matches(message)) {
            lastHello = System.currentTimeMillis();
        } else {
            lastSent = System.currentTimeMillis();
        }
        Random r = new Random();
        List<NodeHandle> all;

        synchronized (Simulation.class) {
            all = new ArrayList<>(simulation.all());
        }

        for (var other : all) {
            if (other == handle || !other.isAlive() || !channel.equals(other.listeningChannel()))
                continue;

            if (r.nextDouble() <= other.reception(handle)) {
                other.receive(message);
            }
        }
    }

    public void receive(Message message) {
        receiveCallback.accept(message);
    }

    @Override
    public void listen(ChannelInfo channelInfo, Observer<Message> observer) {
        this.listeningChannel = channelInfo;
        this.receiveCallback = observer::next;
    }

    @Override
    public String info() {
        return label.get("LoRaSim");
    }
}

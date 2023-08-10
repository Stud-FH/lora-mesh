package simulation;

import model.*;
import model.Module;
import model.Observer;
import model.message.Message;
import model.message.MessageType;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

public class SimulatedLoRaMeshClient implements LoRaMeshClient, Serializable {

    private final Simulation simulation;
    private NodeHandle handle;
    private NodeLabel label;
    private Logger logger;
    private Consumer<Message> receiveCallback;
    ChannelInfo listeningChannel;
    long lastSent;
    long lastHello;
    Logger.Severity logLevel = Logger.Severity.Info;

    public SimulatedLoRaMeshClient(Simulation simulation) {
        this.simulation = simulation;
    }

    @Override
    public void build(Context ctx) {
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

        synchronized(simulation) {
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
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(NodeHandle.class, NodeLabel.class, Logger.class);
    }

    @Override
    public String info() {
        return label.get("LoRaSim");
    }
}

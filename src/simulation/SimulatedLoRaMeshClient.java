package simulation;

import model.*;
import model.Observer;
import model.message.Message;
import model.message.MessageType;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SimulatedLoRaMeshClient implements LoRaMeshClient, Serializable {

    String name;
    double x, y;
    private boolean controller;
    final Map<SimulatedLoRaMeshClient, Double> reception = new HashMap<>();
    transient Node node;
    transient Consumer<Message> receiveCallback;
    transient SimulatedPceClient pce;
    transient SimulatedDataSinkClient data;
    transient ChannelInfo listeningChannel;
    transient long lastSent;
    transient long lastHello;
    Logger.Severity logLevel = Logger.Severity.Info;

    SimulatedLoRaMeshClient(String name, double x, double y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    SimulatedLoRaMeshClient init(long delay) {
        pce = new SimulatedPceClient(this);
        pce.connected = controller;
        data = new SimulatedDataSinkClient();
        data.connected = controller;
        long serialId = UUID.nameUUIDFromBytes(name.getBytes()).getMostSignificantBits();
        node = new Node(
                serialId,
                this,
                data,
                pce,
                new ConsoleLogger(this));
        Simulation.exec.schedule(node::wake, delay, TimeUnit.MILLISECONDS);
        return this;
    }

    public double distance(double x, double y) {
        x -= this.x;
        y -= this.y;
        return Math.sqrt(x*x + y*y);
    }

    public double reception(SimulatedLoRaMeshClient other) {
        return reception.getOrDefault(other, naturalReception(other));
    }

    public double naturalReception(SimulatedLoRaMeshClient other) {
        Random pseudo = new Random((long) (x + 10*y + 100*other.x + 1000*other.y));
        double distance = distance(other.x, other.y);
        double distancePower = 0.2 * Math.pow(1.5, distance * 3);
        return Math.min(0.95, Math.pow(pseudo.nextDouble(), distancePower));
    }

    @Override
    public void enqueue(ChannelInfo channel, Message message) {
        if (message.dataLength() > 12) node.warn("sending long message: %s", message);
        if (MessageType.Hello.matches(message)) {
            lastHello = System.currentTimeMillis();
        } else {
            lastSent = System.currentTimeMillis();
        }
        Random r = new Random();
        List<SimulatedLoRaMeshClient> all;

        synchronized(Simulation.INSTANCE) {
            all = new ArrayList<>(Simulation.INSTANCE.all);
        }

        for (SimulatedLoRaMeshClient other : all) {
            if (other == this || other.node == null || !other.node.isAlive() || !channel.equals(other.listeningChannel))
                continue;

            if (r.nextDouble() <= other.reception(this)) {
//                other.node.debug("receiving from %s: %s", name, message);
                other.receiveCallback.accept(message);
            }
        }
    }

    @Override
    public void listen(ChannelInfo channelInfo, Observer<Message> observer) {
        this.listeningChannel = channelInfo;
        this.receiveCallback = observer::next;
    }

    public void refresh() {
        // todo
        node.refresh();
    }

    public boolean isController() {
        return controller;
    }

    public void setController(boolean controller) {
        this.controller = controller;
        if (pce != null) pce.connected = controller;
        if (data != null) data.connected = controller;
    }

    @Override
    public long getSendingIntervalMillis() {
        return Simulation.INSTANCE.pause? 100000000000L : 1000 / Simulation.INSTANCE.timeFactor;
    }

    @Override
    public String toString() {
        return name + ":  \t" + NodeSnapshot.complete(node);
    }
}

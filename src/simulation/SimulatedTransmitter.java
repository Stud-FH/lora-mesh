package simulation;

import model.*;
import model.message.Message;
import model.message.MessageType;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SimulatedTransmitter implements TransmitterAdapter, Serializable {

    String name;
    double x, y;
    boolean apiConnected;
    final Map<SimulatedTransmitter, Double> reception = new HashMap<>();
    transient Node node;
    transient Consumer<Message> receiveCallback;
    transient SimulatedApi api;
    transient int pseudoFrequency = 0;
    transient long lastSent;
    transient long lastHello;
    Logger.Severity logLevel = Logger.Severity.Info;

    SimulatedTransmitter(String name, double x, double y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    SimulatedTransmitter init(long delay) {
        api = new SimulatedApi(this);
        node = new Node(
                new NodeConfig(this.name, null),
                this,
                api,
                new ConsoleLogger(this));
        Simulation.exec.schedule(node::wakeUp, delay, TimeUnit.MILLISECONDS);
        return this;
    }

    public double distance(double x, double y) {
        x -= this.x;
        y -= this.y;
        return Math.sqrt(x*x + y*y);
    }

    public double reception(SimulatedTransmitter other) {
        return reception.getOrDefault(other, naturalReception(other));
    }

    public double naturalReception(SimulatedTransmitter other) {
        Random pseudo = new Random((long) (x + 10*y + 100*other.x + 1000*other.y));
        double distance = distance(other.x, other.y);
        double distancePower = 0.2 * Math.pow(1.5, distance * 3);
        return Math.min(0.95, Math.pow(pseudo.nextDouble(), distancePower));
    }

    @Override
    public void send(Message message) {
        if (pseudoFrequency == 0) node.error("transmitter not tuned");
        if (MessageType.Hello.matches(message)) lastHello = System.currentTimeMillis();
        else lastSent = System.currentTimeMillis();
        Random r = new Random();
        List<SimulatedTransmitter> all;

        synchronized(Simulation.INSTANCE) {
            all = new ArrayList<>(Simulation.INSTANCE.all);
        }

        for (SimulatedTransmitter other : all) {
            if (other == this || other.node == null || !other.node.isAlive() || other.pseudoFrequency != this.pseudoFrequency)
                continue;

            if (r.nextDouble() <= other.reception(this)) {
//                other.node.debug("receiving from %s: %s", name, message);
                other.receiveCallback.accept(message);
            }
        }
    }

    @Override
    public void setReceiveCallback(Consumer<Message> callback) {
        this.receiveCallback = callback;
    }

    @Override
    public long getSendingIntervalMillis() {
        return Simulation.INSTANCE.pause? 100000000000L : 1000 / Simulation.INSTANCE.timeFactor;
    }

    @Override
    public int getDataLengthLimit() {
        return 9;
    }

    @Override
    public String getTuning() {
        return pseudoFrequency + "";
    }

    @Override
    public void applyDefaultTuning() {
        pseudoFrequency = new Random().nextInt(5);
    }

    @Override
    public void tune(String config) {
        pseudoFrequency = Integer.parseInt(config);
    }

    @Override
    public String tuneStep() {
        pseudoFrequency %= 5;
        pseudoFrequency++;
        return pseudoFrequency + "";
    }

    @Override
    public String toString() {
        return name + ":  \t" + NodeSnapshot.complete(node);
    }
}

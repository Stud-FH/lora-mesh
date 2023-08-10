package simulation;

import local.Node;
import model.*;
import model.Module;
import model.message.Message;

import java.util.Collection;
import java.util.Random;
import java.util.Set;

public class NodeHandle implements Module {

    private Node node;
    private NodeSimulationSpecs specs;
    private SimulatedLoRaMeshClient lora;
    private NodeLabel label;

    @Override
    public void build(Context ctx) {
        node = ctx.resolve(Node.class);
        specs = ctx.resolve(NodeSimulationSpecs.class);
        lora = ctx.resolve(SimulatedLoRaMeshClient.class);
        label = ctx.resolve(NodeLabel.class);
    }

    public NodeSimulationSpecs specs() {
        return specs;
    }

    public void receive(Message message) {
        lora.receive(message);
    }

    public long serialId() {
        return specs.serialId;
    }

    public String label() {
        return label.get();
    }

    public double x() {
        return specs.x;
    }

    public double y() {
        return specs.y;
    }

    public void setX(double x) {
        specs.x = x;
    }

    public void setY(double y) {
        specs.y = y;
    }

    public double distance(double x, double y) {
        x -= this.specs.x;
        y -= this.specs.y;
        return Math.sqrt(x*x + y*y);
    }

    public double distance(NodeHandle other) {
        return distance(other.x(), other.y());
    }

    public byte getNodeId() {
        return node.getNodeId();
    }

    public NodeStatus getStatus() {
        return node.getStatus();
    }

    public boolean isAlive() {
        return node.isAlive();
    }

    public ChannelInfo listeningChannel() {
        return lora.listeningChannel;
    }

    public long lastSent() {
        return lora.lastSent;
    }

    public long lastHello() {
        return lora.lastHello;
    }

    public double reception(NodeHandle other) {
        return specs.reception.getOrDefault(other.serialId(), distanceBasedReception(other));
    }

    public double distanceBasedReception(NodeHandle other) {
        double pseudoRandom = new Random((long) (x() + 10*y() + 100*other.x() + 1000*other.y())).nextDouble();
        double distancePower = 0.2 * Math.pow(1.5, distance(other) * 3);
        return Math.min(0.95, Math.pow(pseudoRandom, distancePower));
    }

    public void resetReception(NodeHandle other) {
        specs.reception.remove(other.serialId());
    }

    public void setReception(NodeHandle other, double value) {
        specs.reception.put(other.serialId(), value);
    }

    public Set<Byte> getRoutingRegistry() {
        return node.getRoutingRegistry();
    }

    public Logger.Severity logLevel() {
        return specs.logLevel;
    }

    public void setLogLevel(Logger.Severity logLevel) {
        specs.logLevel = logLevel;
    }

    public boolean pceDisabled() {
        return specs.pceDisabled;
    }

    public void setPceDisabled(boolean value) {
        specs.pceDisabled = value;
    }

    public boolean dataSinkDisabled() {
        return specs.dataSinkDisabled;
    }

    public void setDataSinkDisabled(boolean value) {
        specs.dataSinkDisabled = value;
    }

    public void feedData(byte[] data) {
        node.feedData(data);
    }

    public void kill() {
        node.error("killed");
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Node.class, NodeSimulationSpecs.class, SimulatedLoRaMeshClient.class, NodeLabel.class);
    }

    @Override
    public String info() {
        return "Node Handle";
    }
}

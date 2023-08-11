package v2.simulation.util;

import v2.core.log.Logger;
import v2.core.domain.ChannelInfo;
import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.domain.message.Message;
import v2.core.domain.node.Node;
import v2.core.domain.node.NodeStatus;
import v2.simulation.domain.NodeSimulationSpecs;
import v2.simulation.impl.SimulatedLoRaMeshClient;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Set;

public class NodeHandle implements Module {

    private Node node;
    private NodeSimulationSpecs specs;
    private SimulatedLoRaMeshClient lora;
    private String label;

    @Override
    public void build(Context ctx) {
        node = ctx.resolve(Node.class);
        specs = ctx.resolve(NodeSimulationSpecs.class);
        lora = ctx.resolve(SimulatedLoRaMeshClient.class);
    }

    public String label() {
        if (label == null) {
            ByteBuffer buf = ByteBuffer.allocate(8);
            buf.putLong(node.id());
            label = new String(buf.array());
        }
        return label;
    }

    public NodeSimulationSpecs specs() {
        return specs;
    }

    public void receive(Message message) {
        lora.receive(message);
    }

    public long id() {
        return specs.id();
    }

    public double x() {
        return specs.x();
    }

    public double y() {
        return specs.y();
    }

    public void setX(double x) {
        specs.setX(x);
    }

    public void setY(double y) {
        specs.setY(y);
    }

    public double distance(double x, double y) {
        x -= this.specs.x();
        y -= this.specs.y();
        return Math.sqrt(x*x + y*y);
    }

    public double distance(NodeHandle other) {
        return distance(other.x(), other.y());
    }

    public byte address() {
        return node.getAddress();
    }

    public NodeStatus getStatus() {
        return node.getStatus();
    }

    public boolean isAlive() {
        return node.isAlive();
    }

    public ChannelInfo listeningChannel() {
        return lora.listeningChannel();
    }

    public long lastSent() {
        return lora.lastSent();
    }

    public long lastHello() {
        return lora.lastHello();
    }

    public double reception(NodeHandle other) {
        return specs.reception.getOrDefault(other.id(), distanceBasedReception(other));
    }

    public double distanceBasedReception(NodeHandle other) {
        double pseudoRandom = new Random((long) (x() + 10*y() + 100*other.x() + 1000*other.y())).nextDouble();
        double distancePower = 0.2 * Math.pow(1.5, distance(other) * 3);
        return Math.min(0.95, Math.pow(pseudoRandom, distancePower));
    }

    public void resetReception(NodeHandle other) {
        specs.reception.remove(other.id());
    }

    public void setReception(NodeHandle other, double value) {
        specs.reception.put(other.id(), value);
    }

    public Set<Byte> getRoutingRegistry() {
        return node.getRoutingRegistry();
    }

    public Logger.Severity logLevel() {
        return specs.logLevel();
    }

    public void setLogLevel(Logger.Severity logLevel) {
        specs.setLogLevel(logLevel);
    }

    public boolean pceDisabled() {
        return specs.pceDisabled();
    }

    public void setPceDisabled(boolean value) {
        specs.setPceDisabled(value);
    }

    public boolean dataSinkDisabled() {
        return specs.dataSinkDisabled();
    }

    public void setDataSinkDisabled(boolean value) {
        specs.setDataSinkDisabled(value);
    }

    public void feedData(byte[] data) {
        node.feedData(data);
    }

    public void kill() {
        node.error("killed");
    }
}

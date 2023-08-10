package simulation;

import local.Node;
import model.Module;
import model.Context;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Set;

public class NodeLabel implements Module {

    private Node node;
    private String label;

    @Override
    public void build(Context ctx) {
        node = ctx.resolve(Node.class);
    }

    @Override
    public void deploy() {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(node.serialId);
        label = buf.toString();
    }

    public String get() {
        return label;
    }

    public String get(String prefix) {
        return String.format("%s %s", prefix, label);
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Node.class);
    }

    @Override
    public String info() {
        return get("Node Label");
    }
}

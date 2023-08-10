package v2.simulation.util;

import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.domain.node.Node;

import java.nio.ByteBuffer;

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
        buf.putLong(node.sid());
        label = new String(buf.array());
    }

    public String get() {
        return label;
    }

    public String get(String prefix) {
        return String.format("%s %s", prefix, label);
    }

}

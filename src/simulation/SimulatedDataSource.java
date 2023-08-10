package simulation;

import local.Node;
import model.Executor;
import model.Module;
import model.Context;

import java.util.Collection;
import java.util.Set;

public class SimulatedDataSource implements Module {

    private Executor exec;
    private Node node;
    private NodeLabel label;

    private int counter = 0;

    @Override
    public void build(Context ctx) {
        exec = ctx.resolve(Executor.class);
        node = ctx.resolve(Node.class);
        label = ctx.resolve(NodeLabel.class);
    }

    private void feedData() {
        if (node.isAlive()) {
            var data = String.format("%s§%d", label.get(), counter).getBytes();
            node.feedData(data);
        }
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Executor.class, Node.class, NodeLabel.class);
    }

    @Override
    public String info() {
        return label.get("SimData");
    }
}

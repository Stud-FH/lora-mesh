package v2.simulation.extension;

import v2.core.concurrency.Executor;
import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.domain.node.Node;
import v2.simulation.util.NodeLabel;

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

    @Override
    public void deploy() {
        // TODO parametrize
        exec.schedulePeriodic(this::feedData, 3000, 4000);
    }

    private void feedData() {
        if (node.isAlive()) {
            var data = String.format("%s§%d", label.get(), counter++).getBytes();
            node.feedData(data);
        }
    }
}

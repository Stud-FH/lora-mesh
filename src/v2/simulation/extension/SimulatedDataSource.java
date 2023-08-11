package v2.simulation.extension;

import v2.core.concurrency.Executor;
import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.domain.node.Node;
import v2.simulation.util.NodeLabel;

public class SimulatedDataSource implements Module {

    private Executor exec;
    private Node node;
    private Config config;

    private int counter = 0;

    @Override
    public void build(Context ctx) {
        exec = ctx.resolve(Executor.class);
        node = ctx.resolve(Node.class);
        config = ctx.resolve(Config.class);
    }

    @Override
    public void deploy() {
        exec.schedulePeriodic(this::feedData, config.dataFeedPeriod(), config.dataFeedPeriod());
    }

    private void feedData() {
        if (node.isAlive()) {
            var data = String.format("%d§%d", node.sid(), counter++).getBytes();
            node.feedData(data);
        }
    }

    public interface Config extends Module {
        long dataFeedPeriod();
    }
}

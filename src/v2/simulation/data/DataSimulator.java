package v2.simulation.data;

import v2.core.common.Counter;
import v2.core.concurrency.Executor;
import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.domain.DataSinkModule;
import v2.core.domain.PceModule;
import v2.core.domain.message.Message;
import v2.core.domain.node.Node;
import v2.core.util.MessageUtil;

import java.util.*;

/**
 * Data source and data sink at the same time
 */
public class DataSimulator implements Module, DataSinkModule {
    private static final Counter counter = new Counter();
    private static final Map<Integer, Node> pending = new HashMap<>();

    private Executor exec;
    private Node node;
    private PceModule pce;
    private Config config;

    @Override
    public void build(Context ctx) {
        exec = ctx.resolve(Executor.class);
        node = ctx.resolve(Node.class);
        pce = ctx.resolve(PceModule.class);
        config = ctx.resolve(Config.class);
    }

    @Override
    public void deploy() {
        exec.schedulePeriodic(this::feedData, config.dataFeedPeriod(), config.dataFeedPeriod());
    }

    private void feedData() {
        if (node.isAlive()) {
            var i = Integer.valueOf((int) counter.increment());
            pending.put(i, node);
            node.feedData(i.toString().getBytes());
        }
    }

    @Override
    public boolean heartbeat() {
        return true;
    }

    @Override
    public Collection<Integer> feed(Message message) {
        Integer i = Integer.parseInt(new String(message.data()));
        pending.remove(i);
        return pce.correspondence(message.getAddress()).registerAndListLosses(message);
    }

    public interface Config extends Module {
        long dataFeedPeriod();
    }
}

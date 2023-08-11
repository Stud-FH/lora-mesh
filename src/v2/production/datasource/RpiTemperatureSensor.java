package v2.production.datasource;

import v2.core.concurrency.Executor;
import v2.shared.integration.CommandLine;
import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.domain.node.Node;

public class RpiTemperatureSensor implements Module {

    private Executor exec;
    private CommandLine cmd;
    private Node node;

    @Override
    public void build(Context ctx) {
        exec = ctx.resolve(Executor.class);
        cmd = ctx.resolve(CommandLine.class);
        node = ctx.resolve(Node.class);
    }

    @Override
    public void deploy() {
        exec.schedulePeriodic(this::feedData, 10000, 3000);
    }

    private void feedData() {
        if (node.isAlive()) {
            var data = cmd.sync("vcgencmd", "measure_temp");
            node.feedData(data);
        }
    }
}

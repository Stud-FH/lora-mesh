package production;

import local.CommandLine;
import local.Node;
import model.ApplicationContext;
import model.Module;
import model.Executor;

import java.util.Collection;
import java.util.Set;

public class RpiTemperatureSensor implements Module {

    private Executor exec;
    private CommandLine cmd;
    private Node node;

    @Override
    public void useContext(ApplicationContext ctx) {
        exec = ctx.resolve(Executor.class);
        cmd = ctx.resolve(CommandLine.class);
        node = ctx.resolve(Node.class);
    }

    @Override
    public void deploy() {
        exec.schedulePeriodic("data feed", this::feedData, 10000, 3000);
    }

    private void feedData() {
        var data = cmd.run("vcgencmd", "measure_temp");
        node.feedData(data);
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(CommandLine.class, Executor.class, Node.class);
    }

    @Override
    public Collection<Class<? extends Module>> providers() {
        return Set.of(RpiTemperatureSensor.class);
    }

    @Override
    public String info() {
        return "Temperature Sensor";
    }
}

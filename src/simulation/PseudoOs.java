package simulation;

import model.Context;
import model.Module;
import model.OsAdapter;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

public class PseudoOs implements OsAdapter {

    private final Simulation simulation;
    private NodeHandle node;

    public PseudoOs(Simulation simulation) {
        this.simulation = simulation;
    }

    @Override
    public void build(Context ctx) {
        node = ctx.resolve(NodeHandle.class);
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of();
    }

    @Override
    public String info() {
        return "Simulated OS";
    }

    @Override
    public Path pwd() {
        return Path.of(".");
    }

    @Override
    public void reboot() {
        simulation.restart(node);
    }
}

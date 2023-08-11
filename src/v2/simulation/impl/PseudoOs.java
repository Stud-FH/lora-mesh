package v2.simulation.impl;

import v2.core.domain.OsAdapter;
import v2.core.context.Context;
import v2.simulation.util.NodeHandle;
import v2.simulation.Simulation;

import java.nio.file.Path;

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
    public Path pwd() {
        return Path.of(".");
    }

    @Override
    public void reboot() {
        simulation.restart(node);
    }
}

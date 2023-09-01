package v2.simulation;

import v2.core.concurrency.Executor;
import v2.core.context.Context;
import v2.core.domain.node.Node;
import v2.core.log.LogMultiplexer;
import v2.core.log.Logger;
import v2.shared.impl.ConsoleLogger;
import v2.shared.impl.FileLogger;
import v2.shared.integration.FileClient;
import v2.shared.measurements.ExecutorInsights;
import v2.shared.measurements.NodeStatistics;
import v2.shared.measurements.ResultsCollector;
import v2.shared.testing.GuardedDataSinkModule;
import v2.shared.testing.GuardedPceModule;
import v2.simulation.data.DataSimulator;
import v2.simulation.domain.NodeSimulationSpecs;
import v2.simulation.gui.ControlPanel;
import v2.simulation.gui.GUI;
import v2.simulation.gui.GraphPanel;
import v2.simulation.impl.PseudoOs;
import v2.simulation.impl.SimulatedLoRaMeshModule;
import v2.simulation.impl.SimulatedPCE;
import v2.simulation.impl.VirtualTimeExecutor;
import v2.simulation.util.NodeHandle;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class Simulation implements ConsoleLogger.Handle, FileClient.Config, VirtualTimeExecutor.Config, DataSimulator.Config, Serializable {
    protected static final long SerialVersionUID = 32;

    public static final Path root = Path.of(String.format("%s/LoraMesh/simulation", System.getenv("LOCALAPPDATA")));
    public static final URI api = URI.create("http://localhost:8080");

    private final List<NodeSimulationSpecs> specsList = new ArrayList<>();
    private transient Context sharedContext;
    private transient Map<NodeSimulationSpecs, Context> context;
    private transient List<NodeHandle> handle;

    private transient NodeHandle selected;
    private transient List<Consumer<NodeHandle>> selectionListeners;
    public int timeControl = 1;
    public String view = "plain";
    public final Logger.Severity logLevel = Logger.Severity.Debug;

    @Override
    public void build(Context ctx) {
        sharedContext = ctx;
        var logger = ctx.resolve(Logger.class);
        var exec = ctx.resolve(Executor.class);

//        ctx.resolve(ExecutorInsights.class).step().subscribe(step -> {
//            if (step >= 10000000) {
//                logger.error("simulation timeout", this);
//                ctx.destroy("simulation ended");
//            }
//        });
    }

    public void deploy() {
        context = new HashMap<>();
        handle = new ArrayList<>();
        selectionListeners = new ArrayList<>();

        specsList.forEach(this::init);
    }

    @Override
    public void preDestroy() {
        System.out.println("finalizing...");
    }

    private void init(NodeSimulationSpecs specs) {
        var nodeHandle = new NodeHandle();
        var nodeCtx = context.put(specs, new Context.Builder(sharedContext)
                .register(specs)
                .register(nodeHandle)
                .register(new Node())
                .register(new PseudoOs(this))
                .register(new LogMultiplexer(new ConsoleLogger(), new FileLogger()))
                .register(new GuardedDataSinkModule(new DataSimulator()))
                .register(new GuardedPceModule(new SimulatedPCE()))
                .register(new SimulatedLoRaMeshModule())
//                .register(new DataSimulator())
                .register(new NodeStatistics())
                .build().deploy());
        handle.add(nodeHandle);
    }

    public void add(String label, double x, double y) {
        var specs = new NodeSimulationSpecs(label, x, y, false);
        specsList.add(specs);
        init(specs);
    }

    public void remove(NodeHandle node) {
        var specs = node.specs();
        context.remove(specs).destroy("node removed");
        handle.remove(node);
        specsList.remove(specs);
    }

    public void restart(NodeHandle node) {
        var specs = node.specs();
        context.remove(specs).destroy("node restarted");
        handle.remove(node);
        init(specs);
    }

    public NodeHandle get(int index) {
        return handle.get(index);
    }

    public Collection<NodeHandle> all() {
        return handle;
    }

    public NodeHandle getSelected() {
        return selected;
    }

    public boolean hasSelected() {
        return selected != null;
    }

    public void select(NodeHandle nodeHandle) {
        selected = nodeHandle;
        for (var listener : selectionListeners) listener.accept(selected);
    }

    public void unselect() {
        selected = null;
        for (var listener : selectionListeners) listener.accept(selected);
    }

    public void addSelectionListener(Consumer<NodeHandle> listener) {
        selectionListeners.add(listener);
    }

    @Override
    public Logger.Severity logLevel() {
        return logLevel;
    }

    @Override
    public Path root() {
        return root;
    }

    @Override
    public int poolSize() {
        return 1;
    }

    @Override
    public double timeFactor() {
        return Math.pow(0.5 , timeControl);
    }

    @Override
    public long dataFeedPeriod() {
        return 5000;
    }

    public static void main(String... args) throws Exception {
        var dir = root.toFile();
        if (!dir.exists() && !dir.mkdirs()) throw new Exception();
        Simulation simulation;
        File file = root.resolve("latest.ser").toFile();
        try {
            FileInputStream fs = new FileInputStream(file);
            ObjectInputStream os = new ObjectInputStream(fs);
            simulation = (Simulation) os.readObject();
            fs.close();
            os.close();
        } catch (Exception e) {
            System.out.printf("no simulation saved under %s\n", file.getAbsolutePath());
            simulation = new Simulation();
            simulation.specsList.add(new NodeSimulationSpecs("ctl", 0, 0, true));
        }

        new Context.Builder()
                .register(simulation)
                .register(new FileClient())
                .register(new VirtualTimeExecutor())
                .register(new ConsoleLogger())
                .register(new GUI())
                .register(new GraphPanel())
                .register(new ControlPanel())
                .register(new ResultsCollector())
                .build().deploy();
    }
}

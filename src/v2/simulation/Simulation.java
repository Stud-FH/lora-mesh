package v2.simulation;

import v2.shared.api.Http;
import v2.shared.api.HttpDataClient;
import v2.shared.api.HttpLogger;
import v2.shared.api.HttpPceClient;
import v2.shared.impl.ConsoleLogger;
import v2.shared.integration.FileClient;
import v2.shared.impl.FileLogger;
import v2.core.log.LogMultiplexer;
import v2.core.log.Logger;
import v2.core.context.Context;
import v2.core.domain.node.Node;
import v2.simulation.domain.NodeSimulationSpecs;
import v2.simulation.datasource.SimulatedDataSource;
import v2.simulation.gui.ControlPanel;
import v2.simulation.gui.GUI;
import v2.simulation.gui.GraphPanel;
import v2.shared.testing.GuardedDataSinkClient;
import v2.shared.testing.GuardedPceClient;
import v2.simulation.impl.PseudoOs;
import v2.simulation.impl.SimulatedLoRaMeshClient;
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

public class Simulation implements ConsoleLogger.Handle, FileClient.Config, VirtualTimeExecutor.Config, Http.Config, SimulatedDataSource.Config, Serializable {

    public static Path root = Path.of(String.format("%s/LoraMesh/simulation", System.getenv("LOCALAPPDATA")));
    public static URI api = URI.create("http://localhost:8080");

    private final List<NodeSimulationSpecs> specsList = new ArrayList<>();
    private transient Context sharedContext;
    private transient Map<NodeSimulationSpecs, Context> context;
    private transient List<NodeHandle> handle;

    private transient NodeHandle selected;
    private transient List<Consumer<NodeHandle>> selectionListeners;
    public int timeControl = 1;
    public String view = "plain";
    public Logger.Severity logLevel = Logger.Severity.Debug;

    @Override
    public void build(Context ctx) {
        sharedContext = ctx;
    }

    public void deploy() {
        context = new HashMap<>();
        handle = new ArrayList<>();
        selectionListeners = new ArrayList<>();

        specsList.forEach(this::init);
    }

    private void init(NodeSimulationSpecs specs) {
        var nodeHandle = new NodeHandle();
        context.put(specs, new Context.Builder(sharedContext)
                .register(specs)
                .register(nodeHandle)
                .register(new Node())
                .register(new PseudoOs(this))
                .register(new LogMultiplexer(new ConsoleLogger(), new FileLogger(), new HttpLogger()))
                .register(new GuardedDataSinkClient(new HttpDataClient()))
                .register(new GuardedPceClient(new HttpPceClient()))
                .register(new SimulatedLoRaMeshClient())
                .register(new SimulatedDataSource())
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
        context.remove(specs).destroy();
        handle.remove(node);
        specsList.remove(specs);
    }

    public void restart(NodeHandle node) {
        var specs = node.specs();
        context.remove(specs).destroy();
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
    public URI api() {
        return api;
    }

    @Override
    public int poolSize() {
        return 5;
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
                .register(new Http())
                .register(new ConsoleLogger())
                .register(new GUI())
                .register(new GraphPanel())
                .register(new ControlPanel())
                .build().deploy();
    }
}

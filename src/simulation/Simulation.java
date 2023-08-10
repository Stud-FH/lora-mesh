package simulation;

import local.*;
import model.LogMultiplexer;
import model.Context;
import model.Logger;
import production.*;
import testing.GuardedDataSinkClient;
import testing.GuardedPceClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class Simulation implements Serializable {

    public static Path root = Path.of(String.format("%s/LoraMesh/simulation", System.getenv("LOCALAPPDATA")));
    public static final String SAVE_PATH = String.format("%s/LoraMesh/simulations/latest.ser", System.getenv("LOCALAPPDATA"));

    private final List<NodeSimulationSpecs> specsList = new ArrayList<>();
    private transient Context sharedContext;
    private transient Map<NodeSimulationSpecs, Context> context;
    private transient List<NodeHandle> handle;

    private transient NodeHandle selected;
    private transient List<Consumer<NodeHandle>> selectionListeners;
    boolean pause = true;
    int timeFactor = 100;
    String view = "plain";


    private void init() throws Exception {
        context = new HashMap<>();
        handle = new ArrayList<>();
        selectionListeners = new ArrayList<>();

        sharedContext = new Context.Builder()
                .register(new VirtualTimeExecutor(5, () -> 100.0 / timeFactor))
                .register(new FileClient(root))
                .register(new Http(new URI("localhost:8080")))
                .register(new ConsoleLogger(() -> Logger.Severity.Warn))
                .register(new GUI())
                .register(new GraphPanel(this))
                .register(new ControlPanel(this))
                .build().deploy();
        specsList.forEach(this::init);
    }

    private void init(NodeSimulationSpecs specs) {
        System.out.println("creating node "+specs.label);
        var nodeHandle = new NodeHandle();
        context.put(specs, new Context.Builder(sharedContext)
                .register(specs)
                .register(nodeHandle)
                .register(new NodeLabel())
                .register(new Node(specs.serialId))
                .register(new PseudoOs(this))
                .register(new LogMultiplexer(new ConsoleLogger(() -> specs.logLevel), new FileLogger(), new HttpLogger()))
                .register(new GuardedDataSinkClient(new HttpDataClient(), () -> !specs.dataSinkDisabled))
                .register(new GuardedPceClient(new HttpPceClient(), () -> !specs.pceDisabled))
                .register(new SimulatedLoRaMeshClient(this))
                .register(new SimulatedDataSource())
                .build());
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
        simulation.init();
    }
}

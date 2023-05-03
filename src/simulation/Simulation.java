package simulation;

import java.io.*;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Consumer;

public class Simulation implements Serializable {

    public static final String SAVE_PATH = String.format("%s/LoraMesh/simulations/latest.ser", System.getenv("LOCALAPPDATA"));
    public static final Simulation INSTANCE;

    public static final ScheduledExecutorService exec = new ScheduledThreadPoolExecutor(1);

    final List<SimulatedTransmitter> all = new ArrayList<>();

    private transient SimulatedTransmitter selected;
    private transient List<Consumer<SimulatedTransmitter>> selectionListeners;
    boolean pause = true;
    int timeFactor = 100;
    String view = "plain";



    public SimulatedTransmitter getSelected() {
        return selected;
    }

    public boolean hasSelected() {
        return selected != null;
    }

    public void select(SimulatedTransmitter simT) {
        selected = simT;
        for (Consumer<SimulatedTransmitter> r : selectionListeners) r.accept(selected);
    }

    public void unselect() {
        selected = null;
        for (Consumer<SimulatedTransmitter> r : selectionListeners) r.accept(selected);
    }

    public void addSelectionListener(Consumer<SimulatedTransmitter> r) {
        selectionListeners.add(r);
    }

    public static void main(String... args) {
        Random r = new Random();
        Simulation simulation = INSTANCE;
        simulation.selectionListeners = new ArrayList<>();
        if (simulation.all.isEmpty()) {
            SimulatedTransmitter controller = new SimulatedTransmitter("ctl", 0, 0);
            controller.apiConnected = true;
            simulation.all.add(controller);
        }
        for (SimulatedTransmitter simT : simulation.all) {
            if (simT.apiConnected) {
                simT.init(0);
            }
            else {
                simT.init(r.nextLong(10000));
            }
        }
        new GUI();
    }

    static {
        Simulation init;
        try {
            File file = new File(SAVE_PATH);
            FileInputStream fs = new FileInputStream(file);
            ObjectInputStream os = new ObjectInputStream(fs);
            init = (Simulation) os.readObject();
            fs.close();
            os.close();
        } catch (Exception e) {
            System.out.printf("no simulation saved under %s\n", SAVE_PATH);
            init = new Simulation();
        }
        INSTANCE = init;
    }
}

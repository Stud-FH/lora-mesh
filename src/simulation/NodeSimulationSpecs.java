package simulation;

import model.Context;
import model.Logger;
import model.Module;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NodeSimulationSpecs implements Module, Serializable {

    public final long serialId;
    public final String label;
    public double x, y;
    public boolean pceDisabled, dataSinkDisabled;
    public Logger.Severity logLevel = Logger.Severity.Warn;

    public final Map<Long, Double> reception = new HashMap<>();

    public NodeSimulationSpecs(String label, double x, double y, boolean controller) {
        this.label = label;
        this.x = x;
        this.y = y;
        pceDisabled = !controller;
        dataSinkDisabled = !controller;

        long tmp = 0;
        byte[] bytes = label.getBytes();
        for (int i = Math.min(8, bytes.length) - 1; i >= 0; i--) {
            tmp = (tmp << 8) + bytes[i];
        }
        serialId = tmp;
    }

    @Override
    public void build(Context ctx) {

    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of();
    }

    @Override
    public String info() {
        return "Specs";
    }
}

package v2.simulation;

import v2.core.log.Logger;
import v2.core.domain.node.Node;
import v2.shared.impl.ConsoleLogger;
import v2.shared.testing.GuardedDataSinkClient;
import v2.shared.testing.GuardedPceClient;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class NodeSimulationSpecs implements Node.Config, ConsoleLogger.Config, GuardedDataSinkClient.Handle, GuardedPceClient.Handle, Serializable {

    private final long sid;
    private double x, y;
    private boolean pceDisabled, dataSinkDisabled;
    private Logger.Severity logLevel = Logger.Severity.Warn;

    public final Map<Long, Double> reception = new HashMap<>();

    public NodeSimulationSpecs(String label, double x, double y, boolean controller) {
        this.x = x;
        this.y = y;
        pceDisabled = !controller;
        dataSinkDisabled = !controller;

        long tmp = 0;
        byte[] bytes = label.getBytes();
        for (int i = 0; i < Math.min(8, bytes.length); i++) {
            tmp = (tmp << 8) + bytes[i];
        }
        sid = tmp;
    }

    @Override
    public long sid() {
        return sid;
    }

    public double x() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double y() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public boolean pceDisabled() {
        return pceDisabled;
    }

    public void setPceDisabled(boolean pceDisabled) {
        this.pceDisabled = pceDisabled;
    }

    public boolean dataSinkDisabled() {
        return dataSinkDisabled;
    }

    public void setDataSinkDisabled(boolean dataSinkDisabled) {
        this.dataSinkDisabled = dataSinkDisabled;
    }

    public Logger.Severity logLevel() {
        return logLevel;
    }

    public void setLogLevel(Logger.Severity logLevel) {
        this.logLevel = logLevel;
    }
}

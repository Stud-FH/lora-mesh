package local;

import model.Logger;
import model.NodeInfo;

import java.util.function.Supplier;

public class ConsoleLogger implements Logger {

    private final Supplier<Severity> logLevelSupplier;
    private final Supplier<String> labelSupplier;

    public ConsoleLogger(Supplier<Severity> logLevelSupplier, Supplier<String> labelSupplier) {
        this.logLevelSupplier = logLevelSupplier;
        this.labelSupplier = labelSupplier;
    }

    @Override
    public void log(Logger.Severity severity, String text, NodeInfo nodeInfo) {
        if (severity.ordinal() < logLevelSupplier.get().ordinal()) return;
//        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        System.out.printf("%s%3s: %s >> %s", setColor(severity), labelSupplier.get(), nodeInfo, text);
        System.out.println("\u001B[0m");
    }

    private String setColor(Logger.Severity severity) {
        switch (severity) {
            case Debug: return "\u001B[61m";
            case Info: return "\u001B[36m";
            case Warn: return "\u001B[33m";
            case Error: return "\u001B[31m";
            default: return "";
        }
    }

}

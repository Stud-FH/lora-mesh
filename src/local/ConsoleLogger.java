package local;

import model.ApplicationContext;
import model.Logger;
import model.Module;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

public class ConsoleLogger implements Logger {

    private final Supplier<Severity> logLevelSupplier;

    public ConsoleLogger(Supplier<Severity> logLevelSupplier) {
        this.logLevelSupplier = logLevelSupplier;
    }

    @Override
    public void log(Logger.Severity severity, String text, Module module) {
        if (severity.ordinal() < logLevelSupplier.get().ordinal()) return;
//        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        System.out.printf("%s%s: %s\u001B[0m\n", setColor(severity), module.info(), text);
    }

    @Override
    public void exception(Exception e, Module module) {
        e.printStackTrace();
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

    @Override
    public String info() {
        return "Console Logger";
    }

    @Override
    public void useContext(ApplicationContext ctx) {
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of();
    }
}

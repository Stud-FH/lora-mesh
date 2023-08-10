package model;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class LogMultiplexer implements Logger {

    private final Collection<Logger> loggers;

    public LogMultiplexer(Logger... loggers) {
        this.loggers = Arrays.asList(loggers);
    }

    @Override
    public void build(Context ctx) {
        loggers.forEach(l -> l.build(ctx));
    }

    @Override
    public void log(Severity severity, String text, Module module) {
        loggers.forEach(l -> l.log(severity, text, module));
    }

    @Override
    public void exception(Exception e, Module module) {
        loggers.forEach(l -> l.exception(e, module));
    }

    @Override
    public Collection<Class<? extends Module>> providers() {
        return loggers.stream().flatMap(l -> l.providers().stream()).collect(Collectors.toSet());
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return loggers.stream().flatMap(l -> l.dependencies().stream()).collect(Collectors.toSet());
    }

    @Override
    public String info() {
        return "Log Multiplexer";
    }
}

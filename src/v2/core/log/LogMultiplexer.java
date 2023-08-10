package v2.core.log;

import v2.core.context.Context;
import v2.core.context.Module;

import java.util.Arrays;
import java.util.Collection;

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
    public void deploy() {
        loggers.forEach(Logger::deploy);
    }

    @Override
    public void destroy() {
        loggers.forEach(Logger::destroy);
    }

    @Override
    public void log(Severity severity, String text, Module module) {
        loggers.forEach(l -> l.log(severity, text, module));
    }

    @Override
    public void exception(Exception e, Module module) {
        loggers.forEach(l -> l.exception(e, module));
    }
}

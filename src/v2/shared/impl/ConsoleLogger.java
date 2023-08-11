package v2.shared.impl;

import v2.core.log.Logger;
import v2.core.context.Context;
import v2.core.context.Module;

public class ConsoleLogger implements Logger {
    private Handle handle;

    @Override
    public void build(Context ctx) {
        handle = ctx.resolve(Handle.class);
    }

    @Override
    public void log(Logger.Severity severity, String text, Module module) {
        if (severity.ordinal() < handle.logLevel().ordinal()) return;
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

    public interface Handle extends Module {
        Logger.Severity logLevel();
    }
}

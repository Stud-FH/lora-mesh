package v2.core.log;

import v2.core.context.Module;

public interface Logger extends Module {

    enum Severity {
        Debug,
        Info,
        Warn,
        Error
    }

    void log(Severity severity, String text, Module module);
    void exception(Exception e, Module module);

    default void debug(String text, Module module) {
        log(Severity.Debug, text, module);
    }

    default void info(String text, Module module) {
        log(Severity.Info, text, module);
    }

    default void warn(String text, Module module) {
        log(Severity.Warn, text, module);
    }

    default void error(String text, Module module) {
        log(Severity.Error, text, module);
    }
}

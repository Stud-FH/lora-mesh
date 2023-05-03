package model;

public interface Logger {

    enum Severity {
        Debug,
        Info,
        Warn,
        Error
    }

    void log(Severity severity, String text, Node node);

    default void debug(String text, Node node) {
        log(Severity.Debug, text, node);
    }

    default void info(String text, Node node) {
        log(Severity.Info, text, node);
    }

    default void warn(String text, Node node) {
        log(Severity.Warn, text, node);
    }

    default void error(String text, Node node) {
        log(Severity.Error, text, node);
    }

}

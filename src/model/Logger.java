package model;

import model.message.NodeInfo;

public interface Logger {

    enum Severity {
        Debug,
        Info,
        Warn,
        Error
    }

    void log(Severity severity, String text, NodeInfo nodeInfo);

    default void debug(String text, NodeInfo nodeInfo) {
        log(Severity.Debug, text, nodeInfo);
    }

    default void info(String text, NodeInfo nodeInfo) {
        log(Severity.Info, text, nodeInfo);
    }

    default void warn(String text, NodeInfo nodeInfo) {
        log(Severity.Warn, text, nodeInfo);
    }

    default void error(String text, NodeInfo nodeInfo) {
        log(Severity.Error, text, nodeInfo);
    }

}

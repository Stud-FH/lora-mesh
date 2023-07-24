package production;

import model.LogEntry;
import model.Logger;
import model.NodeInfo;

import java.util.function.Supplier;

public class HttpLogger implements Logger {

    private final HttpRequestClient httpRequestClient;
    private final Supplier<Logger> backupLogger;

    public HttpLogger(HttpRequestClient httpRequestClient, Supplier<Logger> backupLogger) {
        this.httpRequestClient = httpRequestClient;
        this.backupLogger = backupLogger;
    }

    @Override
    public void log(Severity severity, String text, NodeInfo nodeInfo) {
        var data = new LogEntry();
        data.severity = severity;
        data.nodeInfo = nodeInfo;
        data.data = text.getBytes();
        try {
            httpRequestClient.postResponseVoid("/log", JsonUtil.logEntry(data));
        } catch (Exception e) {
            backupLogger.get().log(severity, text, nodeInfo);
        }
    }
}

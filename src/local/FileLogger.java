package local;

import model.Logger;
import model.NodeInfo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Supplier;

public class FileLogger implements Logger {
    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSSS_Z:");

    private final FileClient fileClient;
    private final Supplier<Logger> backupLogger;

    public FileLogger(FileClient fileClient, Supplier<Logger> backupLogger) {
        this.fileClient = fileClient;
        this.backupLogger = backupLogger;
    }

    @Override
    public void log(Severity severity, String text, NodeInfo nodeInfo) {
        String path = String.format("log/%s-%s.txt", df.format(new Date()), severity);
        try {
            fileClient.write(path, String.format("%s\n%s", nodeInfo, text));
        } catch (Exception e) {
            backupLogger.get().log(severity, text, nodeInfo);
        }
    }
}

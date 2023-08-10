package v2.shared.impl;

import v2.core.log.Logger;
import v2.core.context.Context;
import v2.core.context.Module;
import v2.shared.integration.FileClient;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileLogger implements Logger {
    private static final DateFormat labelDf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSSS_Z:");
    private static final DateFormat filenameDf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
    private static final int FLUSH_THRESHOLD = 10000;

    Path dir = null;

    private FileClient fs;
    private StringBuilder output = new StringBuilder();

    @Override
    public void build(Context ctx) {
        this.fs = ctx.resolve(FileClient.class);
    }

    @Override
    public void postDeploy() {
        dir = fs.create("log");
    }

    @Override
    public void destroy() {
        flush();
    }

    public void flush() {
        if (dir != null) {
            fs.write(dir.resolve(filenameDf.format(new Date())), output.toString());
        }
        output = new StringBuilder();
    }

    @Override
    public void log(Severity severity, String text, Module module) {
        if (output.length() >= FLUSH_THRESHOLD) {
            flush();
        }
        output.append("[").append(severity).append("]")
                .append("\t")
                .append("[").append(labelDf.format(new Date())).append("]")
                .append(" ").append(module.info()).append(": ")
                .append(text)
                .append("\n");
    }

    @Override
    public void exception(Exception e, Module module) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); PrintStream printStream = new PrintStream(outputStream)) {
            e.printStackTrace(printStream);
            log(Severity.Warn, outputStream.toString(), module);
        } catch (Exception ignored) {
        }
    }
}

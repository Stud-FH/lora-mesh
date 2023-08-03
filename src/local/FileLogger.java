package local;

import model.ApplicationContext;
import model.Logger;
import model.Module;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

public class FileLogger implements Logger {
    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSSS_Z:");

    private FileClient fs;
    private FileWriter writer;

    @Override
    public void useContext(ApplicationContext ctx) {
        this.fs = ctx.resolve(FileClient.class);
    }

    @Override
    public void deploy() {
        writer = fs.open("log.txt");
    }

    @Override
    public void destroy() {
        fs.close(writer);
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(FileClient.class);
    }

    @Override
    public void log(Severity severity, String text, Module module) {
        try {
            writer.write(String.format("[%s]\t[%s] %s: %s\n", severity, df.format(new Date()), module.info(), text));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void exception(Exception e, Module module) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); PrintStream printStream = new PrintStream(outputStream)) {
            e.printStackTrace(printStream);
            writer.write(outputStream.toString());
        } catch (Exception ignored) {
        }
    }
    @Override
    public String info() {
        return String.format("File Logger @ %s", fs.cwd);
    }
}

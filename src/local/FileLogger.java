package local;

import model.ApplicationContext;
import model.Logger;
import model.Module;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

public class FileLogger implements Logger {
    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSSS_Z:");

    private FileClient fs;

    @Override
    public void useContext(ApplicationContext ctx) {
        this.fs = ctx.resolve(FileClient.class);
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(FileClient.class);
    }

    @Override
    public void log(Severity severity, String text, Module module) {
        String path = String.format("log/%s-%s.txt", df.format(new Date()), severity);
        try {
            fs.write(path, String.format("%s:\n%s", module.info(), text));
        } catch (Exception ignored) {
        }
    }

    @Override
    public String info() {
        return String.format("File Logger @ %s", fs.cwd);
    }
}

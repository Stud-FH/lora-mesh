package local;

import model.Context;
import model.Logger;
import model.Module;
import model.OsAdapter;

import java.util.Collection;
import java.util.Set;

public class CommandLine implements Module {

    private OsAdapter os;
    private Logger logger;

    @Override
    public String info() {
        return "Command Line";
    }

    @Override
    public void build(Context ctx) {
        os = ctx.resolve(OsAdapter.class);
        logger = ctx.resolve(Logger.class);
    }

    @Override
    public void deploy() {
        var pwd = os.pwd();
        logger.debug(String.format("pwd: %s", pwd), this);
    }

    public byte[] sync(String... command) {
        logger.debug(String.join(" ", command), this);
        try {
            var proc = new ProcessBuilder().command(command).start();
            var result = proc.getInputStream().readAllBytes();
            proc.destroy();
            return result;
        } catch (Exception e) {
            logger.exception(e, this);
            throw new RuntimeException("command failed");
        }
    }

    public Process async(String... command) {
        logger.debug(String.format("running async: %s", String.join(" ", command)), this);
        try {
            return new ProcessBuilder(command).start();
        } catch (Exception e) {
            logger.exception(e, this);
            throw new RuntimeException("command failed");
        }
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(OsAdapter.class, Logger.class);
    }
}

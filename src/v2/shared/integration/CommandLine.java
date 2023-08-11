package v2.shared.integration;

import v2.core.log.Logger;
import v2.core.domain.OsAdapter;
import v2.core.context.Context;
import v2.core.context.Module;

public class CommandLine implements Module {

    private OsAdapter os;
    private Logger logger;

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
}

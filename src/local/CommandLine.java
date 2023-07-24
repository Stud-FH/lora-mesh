package local;

import model.ApplicationContext;
import model.Logger;
import model.Module;

import java.util.Collection;
import java.util.Set;

public class CommandLine implements Module {

    private Logger logger;

    @Override
    public String info() {
        return "Command Line";
    }

    @Override
    public void useContext(ApplicationContext ctx) {
        this.logger = ctx.resolve(Logger.class);
    }

    public byte[] run(String... command) {
        try {
            var proc = new ProcessBuilder().command(command).start();
            var result = proc.getInputStream().readAllBytes();
            proc.destroy();
            return result;
        } catch (Exception e) {
            logger.error(String.format("failed running %s: %s", String.join(" ", command), e.getMessage()), this);
            throw new RuntimeException("command failed");
        }
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Logger.class);
    }

    @Override
    public Collection<Class<? extends Module>> providers() {
        return Set.of(CommandLine.class);
    }
}

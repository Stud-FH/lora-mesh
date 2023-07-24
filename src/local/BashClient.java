package local;

import model.ApplicationContext;
import model.Logger;
import model.Module;

import java.util.Collection;
import java.util.Set;

public class BashClient implements Module {

    private final ApplicationContext ctx;

    public BashClient(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    public byte[] run(String... command) {
        try {
            var proc = new ProcessBuilder().command(command).start();
            var result = proc.getInputStream().readAllBytes();
            proc.destroy();
            return result;
        } catch (Exception e) {
            var logger = ctx.resolve(Logger.class);
            var node = ctx.resolve(Node.class);
            logger.error(String.format("failed running %s: %s", String.join(" ", command), e.getMessage()), node.info());
            throw new RuntimeException("command failed");
        }
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Logger.class, Node.class);
    }

    @Override
    public Collection<Class<? extends Module>> providers() {
        return Set.of(BashClient.class);
    }
}

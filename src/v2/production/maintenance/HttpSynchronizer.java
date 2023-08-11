package v2.production.maintenance;

import v2.core.concurrency.Executor;
import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.domain.node.Node;
import v2.shared.api.Http;
import v2.shared.integration.CommandLine;
import v2.shared.integration.FileClient;

public class HttpSynchronizer implements Module {

    private Http http;
    private FileClient fs;
    private CommandLine bash;
    private Executor exec;
    private Node node;

    @Override
    public void build(Context ctx) {
        http = ctx.resolve(Http.class);
        fs = ctx.resolve(FileClient.class);
        bash = ctx.resolve(CommandLine.class);
        exec = ctx.resolve(Executor.class);
        node = ctx.resolve(Node.class);
    }

    @Override
    public void deploy() {
        exec.schedulePeriodic(this::jarSync, 300000, 0);
        exec.schedulePeriodic(this::statusSync, 300000, 25);
    }

    public void statusSync() {
        var data = bash.sync("ip", "a");
        var response = http.postResponseBinary(String.format("/status/%d", node.id()), data);
        fs.write("config.txt", response);
    }

    public void jarSync() {
        long lastModified = fs.lastModified("node.jar");
        byte[] binary = http.getResponseBinary(String.format("/status?lm=%d", lastModified));
        if (binary.length > 0) {
            fs.write("node.jar", binary);
        }
    }
}

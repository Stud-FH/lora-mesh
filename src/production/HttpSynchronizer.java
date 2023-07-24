package production;

import local.CommandLine;
import local.FileClient;
import model.ApplicationContext;
import model.Module;
import model.Executor;

import java.util.Collection;
import java.util.Set;

public class HttpSynchronizer implements Module {

    private Http http;
    private FileClient fs;
    private CommandLine bash;
    private Executor exec;

    @Override
    public void useContext(ApplicationContext ctx) {
        http = ctx.resolve(Http.class);
        fs = ctx.resolve(FileClient.class);
        bash = ctx.resolve(CommandLine.class);
        exec = ctx.resolve(Executor.class);
    }

    @Override
    public void deploy() {
        exec.schedulePeriodic("jar sync", this::jarSync, 300000, 0);
        exec.schedulePeriodic("status sync", this::statusSync, 300000, 25);
    }

    public void statusSync() {
        var data = bash.run("ip", "a");
        var response = http.postResponseBinary(String.format("/status/%d", Config.serialId), data);
        fs.write("config.txt", response);
    }

    public void jarSync() {
        long lastModified = fs.lastModified("node.jar");
        byte[] binary = http.getResponseBinary(String.format("/status?lm=%d", lastModified));
        if (binary.length > 0) {
            fs.write("node.jar", binary);
        }
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Http.class, FileClient.class, CommandLine.class);
    }

    @Override
    public Collection<Class<? extends Module>> providers() {
        return Set.of(HttpSynchronizer.class);
    }

    @Override
    public String info() {
        return "Http Synchronizer";
    }
}

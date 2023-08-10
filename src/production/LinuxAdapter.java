package production;

import local.CommandLine;
import model.Context;
import model.Module;
import model.OsAdapter;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

public class LinuxAdapter implements OsAdapter {

    CommandLine cmd;

    @Override
    public void build(Context ctx) {
        cmd = ctx.resolve(CommandLine.class);
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(CommandLine.class);
    }

    @Override
    public String info() {
        return "Linux Adapter";
    }

    @Override
    public Path pwd() {
        return Path.of(new String(cmd.sync("echo", "pwd")));
    }

    @Override
    public void reboot() {
        cmd.sync("sudo", "reboot");
    }
}

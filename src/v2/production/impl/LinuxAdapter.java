package v2.production.impl;

import v2.core.context.Context;
import v2.core.domain.OsAdapter;
import v2.shared.integration.CommandLine;

import java.nio.file.Path;

public class LinuxAdapter implements OsAdapter {

    CommandLine cmd;

    @Override
    public void build(Context ctx) {
        cmd = ctx.resolve(CommandLine.class);
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

package v2.core.domain;

import v2.core.context.Module;

import java.nio.file.Path;

public interface OsAdapter extends Module {
    Path pwd();
    void reboot();
}

package model;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

public interface OsAdapter extends Module {
    Path pwd();
    void reboot();

    @Override
    default Collection<Class<? extends Module>> providers() {
        return Set.of(OsAdapter.class, this.getClass());
    }
}

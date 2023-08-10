package model;

import java.util.Collection;
import java.util.Set;

public interface Module {
    void build(Context ctx);

    default void deploy() {

    }

    default Collection<Class<? extends Module>> providers() {
        return Set.of(this.getClass());
    }

    Collection<Class<? extends Module>> dependencies();

    default void destroy() {

    }
    String info();
}

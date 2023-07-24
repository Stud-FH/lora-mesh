package model;

import java.util.Collection;

public interface Module {
    String info();
    void useContext(ApplicationContext ctx);
    Collection<Class<? extends Module>> dependencies();
    Collection<Class<? extends Module>> providers();

    default void deploy() {

    }

    default void destroy() {

    }
}

package model;

import java.util.Collection;

public interface Module {
    Collection<Class<? extends Module>> dependencies();
    Collection<Class<? extends Module>> providers();
}

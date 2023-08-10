package model;

import java.util.Collection;
import java.util.Set;

public interface Executor extends Module {

    void async(Runnable task);
    void schedule(Runnable task, long delay);
    void schedulePeriodic(Runnable task, long period, long delay);

    @Override
    default Collection<Class<? extends Module>> providers() {
        return Set.of(Executor.class);
    }

    @Override
    default String info() {
        return "Executor";
    }

}

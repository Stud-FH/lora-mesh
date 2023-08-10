package v2.core.concurrency;

import v2.core.context.Module;

public interface Executor extends Module {

    void async(Runnable task);
    void schedule(Runnable task, long delay);
    void schedulePeriodic(Runnable task, long period, long delay);

    @Override
    default String info() {
        return "Executor";
    }

}

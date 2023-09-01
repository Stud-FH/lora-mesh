package v2.core.concurrency;

import v2.core.context.Module;

public interface Executor extends Module {

    default CancellationToken async(Runnable task) {
        return schedule(task, 0);
    }
    CancellationToken schedule(Runnable task, long delay);
    CancellationToken schedulePeriodic(Runnable task, long period, long delay);

}

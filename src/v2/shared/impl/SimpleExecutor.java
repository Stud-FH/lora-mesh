package v2.shared.impl;

import v2.core.concurrency.Executor;
import v2.core.context.Context;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SimpleExecutor implements Executor {
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(3);


    public synchronized void async(Runnable task) {
        schedule(task, 0);
    }

    public synchronized void schedule(Runnable task, long delay) {
        executor.schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    public synchronized void schedulePeriodic(Runnable task, long period, long delay) {
        executor.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);
    }

    @Override
    public void build(Context ctx) {
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }
}

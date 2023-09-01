package v2.shared.impl;

import v2.core.concurrency.Executor;
import v2.core.concurrency.CancellationToken;
import v2.core.context.Context;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SimpleExecutor implements Executor {
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(3);

    public synchronized CancellationToken schedule(Runnable task, long delay) {
        var ref = executor.schedule(task, delay, TimeUnit.MILLISECONDS);
        return () -> ref.cancel(true);
    }

    public synchronized CancellationToken schedulePeriodic(Runnable task, long period, long delay) {
        var ref = executor.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);
        return () -> ref.cancel(true);
    }

    @Override
    public void build(Context ctx) {
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }
}

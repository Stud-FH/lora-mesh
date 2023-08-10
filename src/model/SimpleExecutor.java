package model;

import java.util.Collection;
import java.util.Set;
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

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of();
    }
}

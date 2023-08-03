package model;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.*;

public class Executor implements Module {
    private final ScheduledExecutorService sce = new ScheduledThreadPoolExecutor(3);


    public synchronized void async(Runnable task) {
        schedule(task, 0);
    }

    public synchronized void schedule(Runnable task, long delay) {
        sce.schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    public synchronized void schedulePeriodic(Runnable task, long period, long delay) {
        sce.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);
    }

    @Override
    public void useContext(ApplicationContext ctx) {
    }

    @Override
    public void destroy() {
        sce.shutdown();
    }

    @Override
    public String info() {
        return "Executor";
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of();
    }

    @Override
    public Collection<Class<? extends Module>> providers() {
        return Set.of(Executor.class);
    }

}

package model.execution;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Exec {

    private static final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    public static void run(Runnable task) {
        executor.schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    public static void run(Runnable task, long delay) {
        executor.schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    public static void repeat(Runnable task, long period) {
        executor.scheduleAtFixedRate(task, 0, period, TimeUnit.MILLISECONDS);
    }

    public static void repeat(Runnable task, long period, long delay) {
        executor.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);
    }
}

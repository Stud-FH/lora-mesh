package simulation;

import model.Executor;
import model.Logger;
import model.Module;
import model.Context;

import java.util.*;
import java.util.function.Supplier;

public class VirtualTimeExecutor implements Executor {

    private final Supplier<Double> timeFactor;
    private Thread scheduler;
    private Thread[] workerArray;
    private boolean running = true;
    private final Collection<Executable> items = new ArrayList<>();
    private final Queue<Runnable> pending = new LinkedList<>();
    private Logger logger;

    public VirtualTimeExecutor(int threadPoolSize, Supplier<Double> timeFactor) {
        this.workerArray = new Thread[threadPoolSize];
        this.timeFactor = timeFactor;
    }

    public synchronized void async(Runnable task) {
        schedule(task, 0);
    }

    public synchronized void schedule(Runnable task, long delay) {
        items.add(new Executable(task, System.currentTimeMillis(), delay, false));
        notifyAll();
    }

    public synchronized void schedulePeriodic(Runnable task, long period, long delay) {
        items.add(new Executable(task, System.currentTimeMillis() - period + delay, period, true));
        notifyAll();
    }

    @Override
    public void build(Context ctx) {
        logger = ctx.resolve(Logger.class);
    }

    @Override
    public void deploy() {
        scheduler = new Thread(this::schedulerLoop);
        scheduler.start();
        for (int i = 0; i < workerArray.length; i++) {
            workerArray[i] = new Thread(this::workerLoop);
            workerArray[i].start();
        }
    }

    @Override
    public void destroy() {
        running = false;
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of();
    }

    private synchronized Runnable dequeue() throws InterruptedException {
        while (pending.isEmpty()) {
            wait(10);
        }
        return pending.poll();
    }

    private void schedulerLoop() {
        while (running) {
            long now = System.currentTimeMillis();
            double factor = timeFactor.get();
            Collection<Runnable> found = new ArrayList<>();
            Collection<Executable> copy;
            synchronized (this) {
                copy = new ArrayList<>(items);
            }
            long minTargetTime = now + 100;
            for (var item : copy) {
                while (item.scheduledTime + item.delay * factor <= now) {
                    found.add(item.task);
                    if (item.repeat) {
                        item.scheduledTime += item.delay * factor;
                    } else {
                        item.expired = true;
                    }
                }
                minTargetTime = Math.min(minTargetTime, (long) (item.scheduledTime + item.delay * factor));
            }

            synchronized (this) {
                pending.addAll(found);
                items.removeIf(item -> item.expired);
                notifyAll();
                try {
                    wait(minTargetTime - now);
                } catch (InterruptedException e) {
                    logger.exception(e, this);
                }
            }
        }
    }

    private void workerLoop() {
        while (running) {
            try {
                dequeue().run();
            } catch (InterruptedException e) {
                logger.exception(e, this);
            }
        }
    }

    private static class Executable {
        final Runnable task;
        long scheduledTime;
        final long delay;
        final boolean repeat;
        boolean expired = false;

        public Executable(Runnable task, long scheduledTime, long delay, boolean repeat) {
            this.task = task;
            this.scheduledTime = scheduledTime;
            this.delay = delay;
            this.repeat = repeat;
        }
    }
}

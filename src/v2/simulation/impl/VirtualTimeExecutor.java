package v2.simulation.impl;

import v2.core.concurrency.Executor;
import v2.core.log.Logger;
import v2.core.context.Context;
import v2.core.context.Module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public class VirtualTimeExecutor implements Executor {

    private Config config;
    private Thread scheduler;
    private Thread[] workerArray;
    private boolean running = true;
    private boolean paused = true;
    private final Collection<ExecutionItem> items = new ArrayList<>();
    private final Queue<Runnable> pending = new LinkedList<>();
    private Logger logger;

    public boolean paused() {
        return paused;
    }

    public synchronized void pause(boolean value) {
        paused = value;
        notifyAll();
    }

    public synchronized void async(Runnable task) {
        schedule(task, 0);
    }

    public synchronized void schedule(Runnable task, long delay) {
        items.add(new ExecutionItem(task, System.currentTimeMillis(), delay, false));
        notifyAll();
    }

    public synchronized void schedulePeriodic(Runnable task, long period, long delay) {
        items.add(new ExecutionItem(task, System.currentTimeMillis() - period + delay, period, true));
        notifyAll();
    }

    public synchronized void schedulePeriodicStable(Runnable task, long period, long delay) {
        items.add(new StableExecutionItem(task, System.currentTimeMillis() - period + delay, period, true));
        notifyAll();
    }

    @Override
    public void build(Context ctx) {
        config = ctx.resolve(VirtualTimeExecutor.Config.class);
        workerArray = new Thread[config.poolSize()];
        logger = ctx.resolve(Logger.class);
    }

    @Override
    public void deploy() {
        scheduler = new Thread(this::schedulerLoop);
        scheduler.start();
        for (int i = 0; i < workerArray.length; i++) {
            var workerId = i + 1;
            workerArray[i] = new Thread(() -> workerLoop(workerId));
            workerArray[i].start();
        }
    }

    @Override
    public void destroy() {
        running = false;
        try {
            scheduler.join();
        } catch (Exception e) {
            logger.exception(e, this);
        }
        for (var t : workerArray) {
            try {
                t.join();
            } catch (Exception e) {
                logger.exception(e, this);
            }
        }
    }

    private synchronized Runnable dequeue() throws InterruptedException {
        while (pending.isEmpty()) {
            wait(10);
        }
        return pending.poll();
    }

    private void schedulerLoop() {
        while (running) {
            synchronized (this) {
                while (paused) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        logger.exception(e, this);
                    }
                }
            }
//            logger.debug("scheduler cycle", this);
            long now = System.currentTimeMillis();
            double factor = config.timeFactor();
            Collection<Runnable> found = new ArrayList<>();
            Collection<ExecutionItem> copy;
            synchronized (this) {
                copy = new ArrayList<>(items);
            }
            long minTargetTime = now + 100;
            for (var item : copy) {
                while (!item.expired && item.targetTime(factor) <= now) {
                    found.add(item.task);
                    if (item.repeat) {
                        item.increment(factor);
                    } else {
                        item.expired = true;
                    }
                }
                if (!item.expired) {
                    minTargetTime = Math.min(minTargetTime, item.targetTime(factor));
                }
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

    private void workerLoop(int workerId) {
        while (running) {
            try {
                var task = dequeue();
//                logger.debug("t"+workerId+" cycle", this);
                task.run();
            } catch (InterruptedException e) {
                logger.exception(e, this);
            }
        }
    }

    private static class ExecutionItem {
        final Runnable task;
        long scheduledTime;
        final long delay;
        final boolean repeat;
        boolean expired = false;

        public ExecutionItem(Runnable task, long scheduledTime, long delay, boolean repeat) {
            this.task = task;
            this.scheduledTime = scheduledTime;
            this.delay = delay;
            this.repeat = repeat;
        }

        long targetTime(double timeFactor) {
            return (long) (scheduledTime + delay * timeFactor);
        }

        void increment(double timeFactor) {
            scheduledTime += delay * timeFactor;
        }
    }

    private static class StableExecutionItem extends ExecutionItem {

        public StableExecutionItem(Runnable task, long scheduledTime, long delay, boolean repeat) {
            super(task, scheduledTime, delay, repeat);
        }

        @Override
        long targetTime(double timeFactor) {
            return scheduledTime + delay;
        }

        @Override
        void increment(double timeFactor) {
            scheduledTime += delay;
        }
    }

    public interface Config extends Module {
        int poolSize();
        double timeFactor();
    }
}

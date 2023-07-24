package model;

import local.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

public class Executor implements Module {

    private final Collection<ExecutionItem> items = new ArrayList<>();
    private Logger logger;
    private long now;


    public void async(String desc, Runnable task) {
        var item = new ExecutionItem(wrap(desc, task), System.currentTimeMillis());
        items.add(item);
        item.subscribe(Runnable::run);
    }

    public void schedule(String desc, Runnable task, long delay) {
        var item = new ExecutionItem(wrap(desc, task), System.currentTimeMillis() + delay);
        items.add(item);
        item.subscribe(Runnable::run);
    }

    public void schedulePeriodic(String desc, Runnable task, long period, long delay) {
        var item = new PeriodicExecutionItem(wrap(desc, task), System.currentTimeMillis() + delay, period);
        items.add(item);
        item.subscribe(Runnable::run);
    }

    public void scheduleDynamic(String desc, Runnable task, Supplier<Long> targetTime, Supplier<Boolean> done) {
        var item = new DynamicExecutionItem(wrap(desc, task), targetTime, done);
        items.add(item);
        item.subscribe(Runnable::run);
    }

    private Runnable wrap(String desc, Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.warn(String.format("Uncaught Exception by task \"%s\": %s", desc, e.getMessage()), this);
            }
        };
    }

    private void loop() {
        while (true) {
            Collection<ExecutionItem> copy;
            synchronized (this) {
                items.removeIf(ExecutionItem::done);
                copy = new ArrayList<>(items);
            }
            now = System.currentTimeMillis();
            copy.stream().filter(ExecutionItem::ready).forEach(ExecutionItem::trigger);
        }
    }

    @Override
    public void deploy() {
        new Thread(this::loop).start();
    }

    @Override
    public void useContext(ApplicationContext ctx) {
        this.logger = ctx.resolve(Logger.class);
    }

    @Override
    public String info() {
        return "Executor";
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Node.class, Logger.class);
    }

    @Override
    public Collection<Class<? extends Module>> providers() {
        return Set.of(Executor.class);
    }

    private class ExecutionItem extends Subject<Runnable> {
        final Runnable task;
        long targetTime;
        public ExecutionItem(Runnable task, long targetTime) {
            this.task = task;
            this.targetTime = targetTime;
        }
        void trigger() {
            next(task);
            targetTime = Long.MAX_VALUE;
        }
        boolean ready() {
            return targetTime <= now;
        }
        boolean done() {
            return targetTime == Long.MAX_VALUE;
        }
    }

    private class PeriodicExecutionItem extends ExecutionItem {
        final long period;

        public PeriodicExecutionItem(Runnable task, long firstTrigger, long period) {
            super(task, firstTrigger);
            this.period = period;
        }
        void trigger() {
            next(task);
            targetTime += period;
        }
    }

    private class DynamicExecutionItem extends ExecutionItem {
        final Supplier<Long> targetTimeSupplier;
        final Supplier<Boolean> doneSupplier;

        public DynamicExecutionItem(Runnable task, Supplier<Long> targetTimeSupplier, Supplier<Boolean> doneSupplier) {
            super(task, targetTimeSupplier.get());
            this.targetTimeSupplier = targetTimeSupplier;
            this.doneSupplier = doneSupplier;
        }

        void trigger() {
            next(task);
            targetTime += targetTimeSupplier.get();
        }

        @Override
        boolean done() {
            return doneSupplier.get();
        }
    }
}

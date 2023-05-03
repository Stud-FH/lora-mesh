package model.execution;

import model.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class EventHandler {

    private final Node node;
    private final Collection<EventObserver> observers = new ArrayList<>();
    private final Collection<EventScheduler> schedulers = new ArrayList<>();
    public boolean debugEvents = true;

    public EventHandler(Node node) {
        this.node = node;
        new Thread(this::loop).start();
    }

    public synchronized void scheduleWithCooldown(String label, Supplier<Long> cooldown) {
        schedulers.add(new EventScheduler() {
            long last = System.currentTimeMillis();
            @Override
            protected long getDueTime() {
                return last + cooldown.get();
            }

            @Override
            protected void populate(Collection<Event> collection) {
                long now = System.currentTimeMillis();
                if (now >= getDueTime()) {
                    collection.add(new Event(label));
                    last = now;
                }
            }
        });
    }

    public synchronized EventObserver when(Predicate<Event> trigger) {
        var observer = new EventObserver(trigger);
        observers.add(observer);
        notify();
        return observer;
    }

    public synchronized EventObserver when(Supplier<Boolean> trigger) {
        return when(e -> trigger.get());
    }

    public synchronized EventObserver when(String eventName) {
        var observer = new EventObserver(e -> eventName.equals(e.desc))
                .labelled("when " + eventName);
        observers.add(observer);
        notify();
        return observer;
    }

    public synchronized EventObserver immediate(String eventName) {
        schedulers.add(new EventScheduler() {
            @Override
            protected void populate(Collection<Event> collection) {
                collection.add(new Event(eventName));
                invalidate();
            }
        });
        var observer = new EventObserver(e -> eventName.equals(e.desc))
                .labelled("immediate " + eventName)
                .invalidateAfter(1);
        observers.add(observer);
        notify();
        return observer;
    }

    public synchronized EventObserver delayed(String eventName, Supplier<Long> delay) {
        long timestamp = System.currentTimeMillis();
        schedulers.add(new EventScheduler() {
            @Override
            protected void populate(Collection<Event> collection) {
                if (System.currentTimeMillis() < timestamp +delay.get()) return;
                collection.add(new Event(eventName));
                invalidate();
            }
        });
        var observer = new EventObserver(e -> eventName.equals(e.desc))
                .labelled("delayed " + eventName)
                .invalidateAfter(1);
        observers.add(observer);
        notify();
        return observer;
    }

    public synchronized EventObserver scheduled(String eventName, long dueTime) {
        schedulers.add(new EventScheduler() {
            @Override
            protected void populate(Collection<Event> collection) {
                if (System.currentTimeMillis() <= dueTime) {
                    collection.add(new Event(eventName));
                    invalidate();
                }
            }
        });
        var observer = new EventObserver(e -> eventName.equals(e.desc))
                .labelled("scheduled " + eventName)
                .invalidateAfter(1);
        observers.add(observer);
        notify();
        return observer;
    }

    public synchronized void fire(Event event) {
        schedulers.add(new EventScheduler() {
            @Override
            protected void populate(Collection<Event> collection) {
                collection.add(event);
                invalidate();
            }
        });
        notify();
    }

    public synchronized void abortAndReset() {
        observers.clear();
        schedulers.clear();
    }

    private void loop() {
        while (true) {
            Collection<Event> events = new ArrayList<>();
            Collection<EventObserver> observersCopy;
            Collection<EventScheduler> schedulersCopy;
            synchronized (this) {
                observers.removeIf(EventObserver::expired);
                schedulers.removeIf(EventScheduler::expired);
                observersCopy = new ArrayList<>(observers);
                schedulersCopy = new ArrayList<>(schedulers);

                schedulers.forEach(scheduler -> scheduler.populate(events));
            }

            try {

                for (Event event : events) {
                    for (EventObserver observer : observersCopy) {
                        if (observer.observe(event) && debugEvents) {
                            node.debug("\"%s\" observed event \"%s\"", observer, event);
                        }
                    }
                }


                long nextWakeup = System.currentTimeMillis() + 1000;
                for (var scheduler : schedulersCopy) nextWakeup = Math.min(nextWakeup, scheduler.getDueTime());
                long timeout = nextWakeup - System.currentTimeMillis();
                if (timeout > 0) {
                    synchronized (this) {
                        wait(timeout);
                    }
                }
            } catch (Exception e) {
                node.error("exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


}

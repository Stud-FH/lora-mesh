package model.execution;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class EventObserver {

    private Predicate<Event> trigger;
    private Consumer<Event> reaction;
    private Supplier<Boolean> invalidator;
    private String label = "unlabelled";
    private boolean expired = false;

    public EventObserver(Predicate<Event> trigger) {
        this.trigger = trigger;
    }

    public EventObserver then(Consumer<Event> reaction) {
        this.reaction = reaction;
        return this;
    }

    public EventObserver then(Runnable reaction) {
        this.reaction = e -> reaction.run();
        return this;
    }

    public EventObserver invalidateAfter(int n) {
        var tmp = invalidator;
        invalidator = new Supplier<>() {
            int triggersLeft = n;
            final Supplier<Boolean> preCheck = tmp != null? tmp : () -> false;

            @Override
            public Boolean get() {
                return preCheck.get() || (triggersLeft-- <= 0);
            }
        };

        return this;
    }

    public EventObserver labelled(String label) {
        this.label = label;
        return this;
    }

    public EventObserver reactEvery(int n) {
        var tmp = this.trigger;
        this.trigger = new Predicate<>() {
            final Predicate<Event> preCheck = tmp;
            int counter = 0;
            @Override
            public boolean test(Event e) {
                return preCheck.test(e) && (++counter % n) == 0;
            }
        };
        return this;
    }

    public EventObserver invalidateIf(Supplier<Boolean> invalidator) {
        var tmp = this.invalidator;
        this.invalidator = tmp == null? invalidator : new Supplier<>() {
            final Supplier<Boolean> preCheck = tmp;
            @Override
            public Boolean get() {
                return preCheck.get() || invalidator.get();
            }
        };
        return this;
    }

    public boolean observe(Event event) {
        if (expired || reaction == null || !trigger.test(event)) return false;
        if (invalidator != null && invalidator.get()) {
            invalidate();
            return false;
        }

        reaction.accept(event);
        return true;
    }

    public boolean expired() {
        return expired;
    }

    private void invalidate() {
        expired = true;
    }

    @Override
    public String toString() {
        return label;
    }

}
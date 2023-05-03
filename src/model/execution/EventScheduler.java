package model.execution;

import java.util.Collection;

public abstract class EventScheduler {
    private boolean expired = false;

    protected abstract void populate(Collection<Event> collection);

    protected boolean expired() {
        return expired;
    }

    protected void invalidate() {
        expired = true;
    }

    protected long getDueTime() {
        return Long.MAX_VALUE;
    }
}

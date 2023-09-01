package v2.core.common;

public class Counter extends BasicObservable<Long> implements Subject<Long> {

    private long value = 0;

    @Override
    public Long value() {
        return value;
    }

    public synchronized long increment() {
        var prev = value;
        next(++value);
        return prev;
    }
}

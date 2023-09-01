package v2.core.common;

public class BasicSubject<T> extends BasicObservable<T> implements Subject<T> {
    private T value;

    public BasicSubject(T value) {
        this.value = value;
    }

    @Override
    public T value() {
        return value;
    }

    public void set(T value) {
        next(value);
    }

    @Override
    public void next(T value) {
        this.value = value;
        super.next(value);
    }

    @Override
    public String toString() {
        return value().toString();
    }
}

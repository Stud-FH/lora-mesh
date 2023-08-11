package v2.core.common;

public class Subject<T> extends Observable<T> {
    private T value;

    public Subject(T value) {
        this.value = value;
    }

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
}

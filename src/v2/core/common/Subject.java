package v2.core.common;

import java.util.function.Supplier;

public interface Subject<T> extends Observable<T>, Supplier<T> {
    T value();

    @Override
    default T get() {
        return value();
    }
}

package model;

import java.util.function.Consumer;

public interface Observable<T> {

    void subscribe(Consumer<T> subscriber);
}

package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public class Subject<T> implements Observable<T> {
    private final Collection<Consumer<T>> subscribers = new ArrayList<>();

    public void subscribe(Consumer<T> subscriber){
        subscribers.add(subscriber);
    }

    public void next(T value) {
        subscribers.forEach(s -> s.accept(value));
    }
}

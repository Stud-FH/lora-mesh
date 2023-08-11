package v2.core.common;

import java.util.ArrayList;
import java.util.Collection;

public class Observable<T> {
    private Collection<Observer<T>> observers = new ArrayList<>();

    public void next(T value) {
        observers.forEach(o -> o.next(value));
    }

    public Observer.Ref subscribe(Observer<T> observer) {
        observers.add(observer);
        return () -> observers.remove(observer);
    }
}

package v2.core.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BasicObservable<T> implements Observable<T> {
    private final Collection<Observer<T>> observers = new ArrayList<>();

    public void next(T value) {
        List<Observer<T>> copy;
        synchronized(this) {
            copy = new ArrayList<>(observers);
        }
        copy.forEach(o -> o.next(value));
    }

    public Observer.Ref subscribe(Observer<T> observer) {
        observers.add(observer);
        return () -> observers.remove(observer);
    }
}

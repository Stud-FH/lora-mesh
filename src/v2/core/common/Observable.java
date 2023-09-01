package v2.core.common;

public interface Observable<T> {
    Observer.Ref subscribe(Observer<T> observer);
}

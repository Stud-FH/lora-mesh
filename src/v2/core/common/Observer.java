package v2.core.common;

public interface Observer<T> {
    void next(T t);
//    boolean isExpired();
//    void dispose();

    interface Ref {
        void unsubscribe();
    }
}

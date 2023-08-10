package v2.core.domain;

public interface Observer<T> {
    void next(T t);
    boolean isExpired();
    void dispose();
}

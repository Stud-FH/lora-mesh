package model;

public interface Observer<T> {
    void next(T t);
    boolean isExpired();
    void dispose();
}

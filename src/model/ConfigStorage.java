package model;

public interface ConfigStorage {
    String load();
    void save(String config);
}

package api;

import java.util.Optional;

public interface Serfer {
    public void insert(String key, SEntity value);
    public SEntity get(String key);
    public Optional<SEntity> tryGet(String key);
    public boolean delete(String key);
    public boolean contains(String key);
    public void commit();
}

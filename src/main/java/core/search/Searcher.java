package core.search;

public interface Searcher {
    public boolean delete(Key key);
    public void insert(Key key, Value value);
    public Value search(Key key);
}

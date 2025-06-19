package api;

import api.exception.StorageAlreadyExistsException;
import api.exception.StorageNotFoundException;
import core.exception.StorageAccessException;
import core.memory.DiskPageDumper;
import core.page.PageDumper;
import core.search.Key;
import core.search.Searcher;
import core.search.Value;
import core.search.btree.BTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class SerferStorage implements Serfer{

    private final Searcher searcher;
    private final PageDumper dumper;
    private boolean isOpen;

    private SerferStorage(PageDumper dumper) {
        this.dumper = dumper;
        searcher = new BTree(dumper);
        isOpen = true;
    }

    @Override
    public void insert(String key, SEntity value) {
        if (!isOpen) throw new IllegalStateException();
        Key bkey = Key.from(key);
        searcher.insert(bkey, new Value(SEntity.serialize(value)));
    }

    @Override
    public SEntity get(String key) {
        if (!isOpen) throw new IllegalStateException();
        Key bkey = Key.from(key);
        Value result = searcher.search(bkey);
        if (result == null) throw new IllegalArgumentException("cannot find such key " + key);

        return SEntity.deserialize(result.value());
    }

    @Override
    public Optional<SEntity> tryGet(String key) {
        if (!isOpen) throw new IllegalStateException();
        Key bkey = Key.from(key);
        Value result = searcher.search(bkey);
        if (result == null)
            return Optional.empty();

        return Optional.of(SEntity.deserialize(result.value()));
    }

    @Override
    public boolean delete(String key) {
        if (!isOpen) throw new IllegalStateException();
        Key bkey = Key.from(key);
        return searcher.delete(bkey);
    }

    @Override
    public boolean contains(String key) {
        if (!isOpen) throw new IllegalStateException();
        Key bkey = Key.from(key);
        Value result = searcher.search(bkey);

        return result != null;
    }

    @Override
    public void flush() {
        if (!isOpen) throw new IllegalStateException();
        try {
            dumper.close();
            isOpen = false;
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void freeStorage() throws IOException {
        dumper.free();
    }


    public static Serfer open(String filename) throws StorageNotFoundException, IOException {
        Path filePath = Paths.get(filename);
        if (!Files.exists(filePath)) throw new StorageNotFoundException("");

        var dumper = new DiskPageDumper(filePath);
        return new SerferStorage(dumper);
    }

    public static Serfer openOrCreate(String filename) throws IOException {
        Path filePath = Paths.get(filename);
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }

        var dumper = new DiskPageDumper(filePath);
        return new SerferStorage(dumper);
    }

    public static Serfer create(String filename) throws StorageAlreadyExistsException, IOException {
        Path filePath = Paths.get(filename);
        if (Files.exists(filePath)) throw new StorageAlreadyExistsException();

        Files.createFile(filePath);

        var dumper = new DiskPageDumper(filePath);
        return new SerferStorage(dumper);
    }

    public static void freeStorage(String filename) throws StorageNotFoundException, IOException {
        Path filePath = Paths.get(filename);
        if (!Files.exists(filePath)) throw new StorageNotFoundException();

        Files.delete(filePath);
    }
}

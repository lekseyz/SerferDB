package api;

import api.exception.StorageAlreadyExistsException;
import api.exception.StorageNotFoundException;
import core.exception.StorageAccessException;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestSerferStorage {

    private Path tempFile;
    private Serfer storage;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("serfer_test", ".db");
        storage = SerferStorage.openOrCreate(tempFile.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (storage != null) {
            storage.freeStorage();
        }
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testInsertAndGet() {
        storage.insert("key1", SEntity.of(123));
        SEntity entity = storage.get("key1");
        assertEquals(123, entity.asInt().get());
    }

    @Test
    void testTryGetWhenKeyExists() {
        storage.insert("key2", SEntity.of("value"));
        Optional<SEntity> result = storage.tryGet("key2");
        assertTrue(result.isPresent());
        assertEquals("value", result.get().asString().get());
    }

    @Test
    void testTryGetWhenKeyMissing() {
        Optional<SEntity> result = storage.tryGet("unknown_key");
        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteExistingKey() {
        storage.insert("key3", SEntity.of("toDelete"));
        assertTrue(storage.delete("key3"));
        assertFalse(storage.contains("key3"));
    }

    @Test
    void testDeleteMissingKey() {
        assertFalse(storage.delete("nonexistent"));
    }

    @Test
    void testContains() {
        storage.insert("key4", SEntity.of(999));
        assertTrue(storage.contains("key4"));
        assertFalse(storage.contains("absent"));
    }

    @Test
    void testFlushPreventsFurtherUse() {
        storage.insert("before", SEntity.of("ok"));
        storage.flush();

        assertThrows(IllegalStateException.class, () -> storage.insert("after", SEntity.of("fail")));
    }

    @Test
    void testOpenThrowsIfNotExists() {
        Path nonexistent = tempFile.resolveSibling("not_exists_" + System.nanoTime());
        assertThrows(StorageNotFoundException.class, () -> SerferStorage.open(nonexistent.toString()));
    }

    @Test
    void testCreateThrowsIfAlreadyExists() throws IOException {
        Path existing = Files.createTempFile("serfer_existing", ".db");
        assertThrows(StorageAlreadyExistsException.class, () -> SerferStorage.create(existing.toString()));
        Files.delete(existing);
    }

    @Test
    void testOpenOrCreateCreatesFileIfNotExists() throws IOException {
        Path newFile = tempFile.resolveSibling("new_db_" + System.nanoTime());
        assertFalse(Files.exists(newFile));

        Serfer s = SerferStorage.openOrCreate(newFile.toString());
        assertTrue(Files.exists(newFile));

        s.freeStorage();
        Files.deleteIfExists(newFile);
    }

    @Test
    void testFreeStorageDeletesFile() throws IOException {
        Path path = tempFile.resolveSibling("freeme.db");
        Files.createFile(path);

        SerferStorage.freeStorage(path.toString());
        assertFalse(Files.exists(path));
    }

    @Test
    void testFreeStorageThrowsIfNotExists() {
        Path missing = tempFile.resolveSibling("missing_" + System.nanoTime());
        assertThrows(StorageNotFoundException.class, () -> SerferStorage.freeStorage(missing.toString()));
    }
}

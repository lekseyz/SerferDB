package core.search.btree;

import core.memory.DiskPageDumper;
import core.search.Key;
import core.search.Value;
import core.search.btree.utils.RandomState;
import core.search.btree.utils.TesterDumper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static core.search.btree.utils.ByteArrayWrapper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestBTreeDumper {
    static RandomState state;

    DiskPageDumper dumper;
    BTree tree;

    @BeforeEach
    void setUpTest() throws IOException {
        Path dataPath = Paths.get("_test_data.dump");
        Path walPath = Paths.get("_test_wal.dump");

        for (var path : List.of(new Path[]{dataPath, walPath})) {
            if (Files.exists(path)){
                Files.delete(path);
            }
        }
        dumper = new DiskPageDumper(dataPath);
        tree = new BTree(dumper);
    }

    @AfterEach
    void freeTest() throws IOException {
        dumper.close();
    }

    @BeforeAll
    static void setRandom() {
        state = new RandomState(42);
    }

    @Test
    void testInsertDelete() {
        tree.insert(getKey(1), getValue("abc"));
        tree.insert(getKey(2), getValue("bcd"));

        assertArrayEquals(tree.search(getKey(1)).value(), toBytes("abc"));
        assertArrayEquals(tree.search(getKey(2)).value(), toBytes("bcd"));

        tree.delete(getKey(1));

        assertNull(tree.search(getKey(1)));
    }

    @Test
    void testEdgeKey() {
        tree.insert(getKey(0), getValue("smallest int key"));
        tree.insert(getKey(-1), getValue("biggest int key"));

        assertArrayEquals(tree.search(getKey(0)).value(), toBytes("smallest int key"));
        assertArrayEquals(tree.search(getKey(-1)).value(), toBytes("biggest int key"));
        assertEquals(tree.search(Key.NullKey()), Value.NullValue()); //Check that null key and null value is presents

        tree.delete(getKey(0));

        assertNull(tree.search(getKey(0)));
        assertArrayEquals(tree.search(getKey(-1)).value(), toBytes("biggest int key"));

        tree.delete(getKey(-1));

        assertNull(tree.search(getKey(0)));
        assertNull(tree.search(getKey(-1)));
        assertNull(tree.search(Key.NullKey()));
    }

    @Test
    void testDoubleInsert() {
        tree.insert(getKey(1), getValue("abc"));
        assertEquals(getValue("abc"), tree.search(getKey(1)));
        tree.insert(getKey(1), getValue("bcd"));
        assertEquals(getValue("bcd"), tree.search(getKey(1)));
    }

    @Test
    void testStressTest() {
        Map<Key, Value> testTree = new TreeMap<>();

        for (int i = 0; i < 100_000; i++) {
            var key = getKey(state.stringGen(Key.MAX_KEY_SIZE - 1));
            var value = getValue(state.stringGen(Value.MAX_VALUE_SIZE - 1));
            testTree.put(key, value);
            tree.insert(key, value);
        }
        var allItems = new java.util.ArrayList<>(testTree.entrySet().stream().toList());
        Collections.shuffle(allItems, state.getRandom());
        int i = 0;
        for (var pair :  allItems) {
            Key copy = pair.getKey().getCopy();
            assertArrayEquals(testTree.get(copy).value(), tree.search(copy).value());

            testTree.remove(copy);
            tree.delete(copy);

            assertNull(testTree.get(copy));
            assertNull(tree.search(copy));
        }
    }
}

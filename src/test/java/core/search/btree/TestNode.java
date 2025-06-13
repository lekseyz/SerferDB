package core.search.btree;
import core.page.PagingConstants;
import core.search.Key;
import core.search.Value;
import core.search.btree.utils.RandomState;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static core.search.btree.utils.ByteArrayWrapper.*;
import static core.search.btree.utils.KeyValueAssert.*;

public class TestNode {
    static RandomState state;

    @BeforeAll
    static void setRandom() {
        state = new RandomState(42);
    }

    @Test
    void testLeafAddRemove() {
        Node node = new Node(true);
        node.leafUpdate(Key.NullKey(), Value.NullValue());
        node.leafUpdate(getKey(1), getValue("abc"));
        assertKeyValue(node, 1, "abc");

        node.leafUpdate(getKey(3), getValue("bcd"));
        node.leafUpdate(getKey(2), getValue("bca"));

        assertKeyValue(node, 1, "abc");
        assertKeyValue(node, 2, "bca");
        assertKeyValue(node, 3, "bcd");

        node.leafUpdate(getKey(1), getValue("ccc"));

        assertKeyValue(node, 1, "ccc");
        assertKeyValue(node, 2, "bca");
        assertKeyValue(node, 3, "bcd");

        node.leafDelete(getKey(1));

        assertNull(node.getKeyValue(getKey(1)));
        assertEquals(3, node.getKeys().size());

        assertTrue(node.leafDelete(getKey(2)));
        assertFalse(node.leafDelete(getKey(2)));
        node.leafDelete(getKey(3));

        assertNull(node.getKeyValue(getKey(1)));
        assertNull(node.getKeyValue(getKey(2)));
        assertNull(node.getKeyValue(getKey(3)));
    }

    @Test
    void testNodeAddRemove() {
        Node node = new Node(false);
        node.nodeUpdate(Key.NullKey(), -1);
        node.nodeUpdate(getKey("abc"), 10);

        assertKeyRef(node, "abc", 10);

        node.nodeUpdate(getKey(20), 20);
        node.nodeUpdate(getKey("string key"), 100);

        assertKeyRef(node, "string key", 100);
        assertKeyRef(node, 20, 20);

        assertTrue(node.nodeDelete(getKey("abc")));
        assertKeyRef(node, "abc", 20);

        assertTrue(node.nodeDelete(getKey("string key")));
        assertTrue(node.nodeDelete(getKey(20)));

        assertKeyRef(node, "abc", -1);
        assertKeyRef(node, "string key", -1);
        assertKeyRef(node, 20, -1);
    }

    @Test
    void testNodeSplit() {
        Node node = new Node(true);
        node.leafUpdate(Key.NullKey(), Value.NullValue());
        Map<String, String> map = new TreeMap<>();

        while(node.nodeSize() <= PagingConstants.MAX_PAGE_SIZE) {
            String key = state.stringGen(100);
            String value = state.stringGen(300);
            node.leafUpdate(getKey(key), getValue((value)));
            map.put(key, value);
        }

        var split = Node.split(node);
        assertFalse(split.isEmpty());

        for (var kv : map.entrySet()) {
            assertTrue(split.stream().anyMatch(node1 -> node1.leafDelete(getKey(kv.getKey()))));
        }
        assertTrue(split.stream().anyMatch(s -> s.leafDelete(Key.NullKey())));
        assertTrue(split.stream().allMatch(node1 -> node1.getKeys().isEmpty()));
    }

    @Test
    void testNodeEncodeDecode() {
        Node node = new Node(true);

        for (int i = 0; i < 5; i++) {
            node.leafUpdate(getKey(state.stringGen(10)), getValue(state.stringGen(20)));
        }

        var buff = Node.encode(node);

        assertEquals(PagingConstants.MAX_PAGE_SIZE, buff.capacity());

        Node newNode = Node.decode(buff);

        assertTrue(newNode.getKeys().size() == newNode.getValues().size());
        assertEquals(node.getKeys().size(), newNode.getKeys().size());

        for (int i = 0; i < node.getKeys().size(); i++) {
            assertEquals(0, node.getKeys().get(i).compareTo(newNode.getKeys().get(i)));
            assertEquals(node.getValues().get(i), newNode.getValues().get(i));
        }
    }
}

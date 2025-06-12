package core.search.BTree;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static core.search.BTree.utils.ByteArrayWrapper.*;

public class TestNode {
    static Random random;

    Key getKey(int value) {
        return new Key(toBytes(value));
    }

    Key getKey(String value) {
        return new Key(toBytes(value));
    }

    Value getValue(int value) {
        return new Value(toBytes(value));
    }

    Value getValue(String value) {
        return new Value(toBytes(value));
    }

    void assertKeyValue(Node node, int key, int value) {
        assertEquals(value, getInt(node.getKeyValue(getKey(key)).value));
    }

    void assertKeyValue(Node node, int key, String value) {
        assertEquals(value, getString(node.getKeyValue(getKey(key)).value));
    }

    void assertKeyValue(Node node, String key, String value) {
        assertEquals(value, getString(node.getKeyValue(getKey(key)).value));
    }

    void assertKeyValue(Node node, String key, int value) {
        assertEquals(value, getInt(node.getKeyValue(getKey(key)).value));
    }

    void assertKeyRef(Node node, int key, int ref) {
        assertEquals(ref, node.getChildRef(getKey(key)));
    }

    void assertKeyRef(Node node, String key, int ref) {
        assertEquals(ref, node.getChildRef(getKey(key)));
    }

    String stringGen(int length) {
        return random.ints('a', 'z' + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    @BeforeAll
    static void setRandom() {
        random = new Random(42);
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
        Map<String, String> map = new TreeMap<>();

        while(node.nodeSize() <= Node.PAGE_SIZE) {
            String key = stringGen(100);
            String value = stringGen(300);
            node.leafUpdate(getKey(key), getValue((value)));
            map.put(key, value);
        }

        var split = Node.split(node);
        assertFalse(split.isEmpty());

        for (var kv : map.entrySet()) {
            assertTrue(split.stream().anyMatch(node1 -> node1.leafDelete(getKey(kv.getKey()))));
        }

        assertTrue(split.stream().allMatch(node1 -> node1.getKeys().isEmpty()));
    }

    @Test
    void testNodeEncodeDecode() throws Node.NodeSizeException, Node.ValueSizeException, Node.KeySizeException {
        Node node = new Node(true);

        for (int i = 0; i < 5; i++) {
            node.leafUpdate(getKey(stringGen(10)), getValue(stringGen(20)));
        }

        var buff = Node.encode(node);

        assertEquals(Node.PAGE_SIZE, buff.capacity());

        Node newNode = Node.decode(buff);

        assertTrue(newNode.getKeys().size() == newNode.getValues().size());
        assertEquals(node.getKeys().size(), newNode.getKeys().size());

        for (int i = 0; i < node.getKeys().size(); i++) {
            assertEquals(0, node.getKeys().get(i).compareTo(newNode.getKeys().get(i)));
            assertArrayEquals(node.getValues().get(i).value, newNode.getValues().get(i).value);
        }
    }
}

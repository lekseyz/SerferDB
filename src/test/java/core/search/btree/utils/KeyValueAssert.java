package core.search.btree.utils;

import core.search.btree.Node;

import static core.search.btree.utils.ByteArrayWrapper.*;
import static core.search.btree.utils.ByteArrayWrapper.getKey;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeyValueAssert {
    public static void assertKeyValue(Node node, int key, int value) {
        assertEquals(value, getInt(node.getKeyValue(getKey(key)).value()));
    }

    public static void assertKeyValue(Node node, int key, String value) {
        assertEquals(value, getString(node.getKeyValue(getKey(key)).value()));
    }

    public static void assertKeyValue(Node node, String key, String value) {
        assertEquals(value, getString(node.getKeyValue(getKey(key)).value()));
    }

    public static void assertKeyValue(Node node, String key, int value) {
        assertEquals(value, getInt(node.getKeyValue(getKey(key)).value()));
    }

    public static void assertKeyRef(Node node, int key, int ref) {
        assertEquals(ref, node.getChildRef(getKey(key)));
    }

    public static void assertKeyRef(Node node, String key, int ref) {
        assertEquals(ref, node.getChildRef(getKey(key)));
    }

    private KeyValueAssert() {

    }
}

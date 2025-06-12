package core.search.BTree;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/*
Node structure in buffer:
| node type | keys amount | ||key size| |key|| * amount | |children ref| * amount | ||value size| |value|| * amount | empty |
|    1 b    |      1b     | ||   1b   | |Nb || * amount | |    4b      | * amount | ||    1b    | |  Nb || * amount |  ...  |

if node is leaf, then children ref empty, else value part is empty
 */
public class Node {
    // Constants for page sizing
    public static final int PAGE_SIZE = 1024 * 4;
    public static final int KEY_MAX_SIZE = 256;
    public static final int VAL_MAX_SIZE = 512;

    //region Node fields
    public static class Key implements Comparable<Key>{
        public byte[] key;

        public Key(byte[] key) {
            this.key = key;
        }

        public Key() {
            this.key = null;
        }

        public static Key NullKey() {
            return new Key(new byte[]{0});
        }

        @Override
        public int compareTo(Key o) {
            int minLen = Math.min(this.key.length, o.key.length);

            for (int i = 0; i < minLen; i++) {
                int cmp = Integer.compare(
                        Byte.toUnsignedInt(this.key[i]),
                        Byte.toUnsignedInt(o.key[i])
                );
                if (cmp != 0) return cmp;
            }

            return Integer.compare(this.key.length, o.key.length);
        }
    }
    public static class Value {
        public byte[] value;

        public Value(byte[] value) {
            this.value = value;
        }

        public Value() {
            this.value = null;
        }

        public static Value NullValue() {
            return new Value(new byte[]{0});
        }
    }

    public static class NodeSizeException extends Exception {}
    public static class ValueSizeException extends Exception {}
    public static class KeySizeException extends Exception {}

    public boolean isLeaf;
    public List<Key> keys;
    public List<Integer> childrenRefs;
    public List<Value> values;
    //endregion

    public Node(boolean isLeaf) {
        this.isLeaf = isLeaf;
        keys = new ArrayList<>();
        childrenRefs = new ArrayList<>();
        values = new ArrayList<>();
    }

    private Node() {
        keys = new ArrayList<>();
        childrenRefs = new ArrayList<>();
        values = new ArrayList<>();
    }


    //region Byte buffer encoding decoding
    public static ByteBuffer Encode(Node node) throws NodeSizeException, KeySizeException, ValueSizeException {
        if (node.NodeSize() > PAGE_SIZE) throw new NodeSizeException();

        var buff = ByteBuffer.allocate(PAGE_SIZE);
        buff.put((byte) (node.isLeaf ? 1 : 0));
        buff.putShort((short) node.keys.size());

        for (var key : node.keys) {
            if (key.key.length > KEY_MAX_SIZE) throw new KeySizeException();

            buff.putShort((short)key.key.length);
            buff.put(key.key);
        }
        if (node.isLeaf){
            for (var value : node.values) {
                if (value.value.length > VAL_MAX_SIZE) throw new ValueSizeException();

                buff.putShort((short)value.value.length);
                buff.put(value.value);
            }
        } else {
            for (int ref : node.childrenRefs) {
                buff.putInt(ref);
            }
        }

        return buff;
    }

    public static Node Decode(ByteBuffer buffer) {
        Node node = new Node();
        node.isLeaf = buffer.get() == 1;
        short keysCount = buffer.getShort();

        node.keys = new ArrayList<>();
        for (int i = 0; i < keysCount; i++) {
            short keyLen = buffer.getShort();
            var key = new Key();
            node.keys.add(key);
            key.key = new byte[keyLen];
            buffer.get(key.key);
        }

        if (node.isLeaf) {
            node.values = new ArrayList<>();

            for (int i = 0; i < keysCount; i++) {
                short valueLen = buffer.getShort();
                var value = new Value();
                node.values.add(value);
                value.value = new byte[valueLen];
                buffer.get(value.value);
            }
        } else {
            node.childrenRefs = new ArrayList<>();
            for (int i = 0; i < keysCount; i++) {
                var ref = buffer.getInt();
                node.childrenRefs.add(ref);
            }
        }
        return node;
    }
    //endregion

    public int NodeSize() {
        int size = 1 + 2; // flag + keys count
        for (var key : this.keys) {
            size += 2 + key.key.length;
        }
        if (this.isLeaf) {
            for (var value : this.values) {
                size += 2 + value.value.length;
            }
        } else {
            size += this.childrenRefs.size() * 4; // amount of children * size of ref
        }

        return size;
    }

    public int GetKeyIndex(Key key) {
        int newKeyIndex = Collections.binarySearch(this.keys, key);
        if (newKeyIndex >= 0) return newKeyIndex;
        return -newKeyIndex - 2;
    }

    //region Node insertions and updates
    public void NodeUpdate(Key key, int child) {
        NodeUpdate(key, child, null);
    }

    public void NodeUpdate(Key key, int child, Key newKey) {
        if (this.isLeaf) return; // TODO: error
        if (this.keys.isEmpty()) {
            this.keys.add(key);
            this.childrenRefs.add(child);
        }

        int idx = GetKeyIndex(key);
        if (newKey != null) {
            this.keys.set(idx, newKey);
            this.childrenRefs.set(idx, child);
        }
        if (this.keys.get(idx).compareTo(key) == 0)
            this.childrenRefs.set(idx, child);
        else {
            this.keys.add(idx + 1, key);
            this.childrenRefs.add(idx + 1, child);
        }
    }

    public void LeafUpdate(Key key, Value value) {
        if (!this.isLeaf) return; // TODO: error
        if (this.keys.isEmpty()) {
            this.keys.add(key);
            this.values.add(value);
            return;
        }

        int idx = GetKeyIndex(key);
        if (this.keys.get(idx).compareTo(key) == 0)
            this.values.set(idx, value);
        else {
            this.keys.add(idx + 1, key);
            this.values.add(idx + 1, value);
        }
    }
    //endregion

    //region Node deletion
    public boolean LeafDelete(Key key) {
        if (!this.isLeaf) return false; // TODO: error
        if (this.keys.isEmpty())
            return false;

        int idx = GetKeyIndex(key);
        if (this.keys.get(idx).compareTo(key) == 0) {
            this.keys.remove(idx);
            this.values.remove(idx);
            return true;
        }
        return false;
    }

    public boolean NodeDelete(Key key) {
        if (this.isLeaf) return false;
        if (this.keys.isEmpty())
            return false;

        int idx = GetKeyIndex(key);
        this.keys.remove(idx);
        this.childrenRefs.remove(idx);
        return true;
    }
    //endregion

    //region Work with children
    public int GetChildRef(Key key) {
        if (this.isLeaf) return -1; //TODO: error

        int idx = GetKeyIndex(key);
        return this.childrenRefs.get(idx);
    }

    public void InsertSplitChildren(List<Node> children, List<Integer> refs) {
        for (int i = 0; i < children.size(); i++) {
            NodeUpdate(children.get(i).keys.getFirst(), refs.get(i));
        }
    }

    public Node MergeTwoChildren(Node left, Node right) {
        assert left.isLeaf == right.isLeaf : "Incompatible children types";

        left.keys.addAll(right.keys);
        if (left.isLeaf) {
            left.values.addAll(right.values);
        } else {
            left.childrenRefs.addAll(right.childrenRefs);
        }
        int idx = GetKeyIndex(right.keys.getFirst());
        this.keys.remove(idx);
        this.childrenRefs.remove(idx);
        return left;
    }

    //region Node splitting
    public static List<Node> split(Node old) {
        if (old.NodeSize() <= PAGE_SIZE) {
            return Collections.singletonList(old);
        }

        Node[] two = split2(old);
        Node left = two[0], right = two[1];

        if (left.NodeSize() <= PAGE_SIZE) {
            return List.of(left, right);
        }

        Node[] leftSplit = split2(left);
        return List.of(leftSplit[0], leftSplit[1], right);
    }

    private static Node[] split2(Node old) {
        int nkeys = old.keys.size();
        if (nkeys < 2) throw new IllegalArgumentException("need >=2 keys to split");

        int nleft = nkeys / 2;

        while (nleft > 0 && partialSize(old, 0, nleft) > PAGE_SIZE) {
            nleft--;
        }
        if (nleft < 1) nleft = 1;

        while (nleft < nkeys && partialSize(old, nleft, nkeys) > PAGE_SIZE) {
            nleft++;
        }
        if (nleft >= nkeys) nleft = nkeys - 1;

        Node left = sliceNode(old, 0, nleft);
        Node right = sliceNode(old, nleft, nkeys);
        return new Node[]{ left, right };
    }

    private static int partialSize(Node src, int from, int to) {
        int size = 1 + 2; // 1 байт флага + 2 байта количества ключей
        for (int i = from; i < to; i++) {
            size += 2 + src.keys.get(i).key.length;
        }
        if (src.isLeaf) {
            for (int i = from; i < to; i++) {
                size += 2 + src.values.get(i).value.length;
            }
        } else {
            size += (to - from) * 4;
        }
        return size;
    }

    private static Node sliceNode(Node src, int from, int to) {
        Node dst = new Node();
        dst.isLeaf = src.isLeaf;
        dst.keys = new ArrayList<>(src.keys.subList(from, to));

        if (src.isLeaf) {
            dst.values = new ArrayList<>(src.values.subList(from, to));
        } else {
            dst.childrenRefs = new ArrayList<>(
                    src.childrenRefs.subList(from, to)
            );
        }
        return dst;
    }
    //endregion
}

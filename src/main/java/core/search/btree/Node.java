package core.search.btree;

import core.page.PagingConstants;
import core.search.Key;
import core.search.Value;

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
    //region Node fields

    private boolean isLeaf;
    private List<Key> keys;
    private List<Integer> childrenRefs;
    private List<Value> values;
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
    public static ByteBuffer encode(Node node) {
        if (node.nodeSize() > PagingConstants.PAGE_SIZE) throw new RuntimeException("node to big");

        var buff = ByteBuffer.allocate(PagingConstants.PAGE_SIZE);
        buff.put((byte) (node.isLeaf ? 1 : 0));
        buff.putShort((short) node.keys.size());

        for (var key : node.keys) {
            buff.putShort((short)key.getKeyLength());
            buff.put(key.key());
        }
        if (node.isLeaf){
            for (var value : node.values) {
                buff.putShort((short)value.value().length);
                buff.put(value.value());
            }
        } else {
            for (int ref : node.childrenRefs) {
                buff.putInt(ref);
            }
        }

        return buff.flip();
    }

    public static Node decode(ByteBuffer buffer) {
        Node node = new Node();
        node.isLeaf = buffer.get() == 1;
        short keysCount = buffer.getShort();

        node.keys = new ArrayList<>();
        for (int i = 0; i < keysCount; i++) {
            short keyLen = buffer.getShort();
            var b = new byte[keyLen];
            buffer.get(b);
            var key = new Key(b);
            node.keys.add(key);
        }

        if (node.isLeaf) {
            node.values = new ArrayList<>();

            for (int i = 0; i < keysCount; i++) {
                short valueLen = buffer.getShort();
                var b = new byte[valueLen];
                buffer.get(b);
                var value = new Value(b);
                node.values.add(value);
            }
        } else {
            node.childrenRefs = new ArrayList<>();
            for (int i = 0; i < keysCount; i++) {
                var ref = buffer.getInt();
                node.childrenRefs.add(ref);
            }
        }

        buffer.flip();
        return node;
    }
    //endregion

    //region Size methods
    public int nodeSize() {
        int size = 1 + 2; // flag + keys count
        for (var key : this.keys) {
            size += 2 + key.getKeyLength();
        }
        if (this.isLeaf) {
            for (var value : this.values) {
                size += 2 + value.value().length;
            }
        } else {
            size += this.keys.size() * 4; // amount of children * size of ref
        }

        return size;
    }

    public boolean isMergingSize() {
        int size = this.nodeSize();
        return size <= PagingConstants.PAGE_SIZE / 4;
    }
    //endregion

    private int getKeyIndex(Key key) {
        int newKeyIndex = Collections.binarySearch(this.keys, key);
        if (newKeyIndex >= 0) return newKeyIndex;
        return -newKeyIndex - 2;
    }

    public Value getKeyValue(Key key) {
        if (!this.isLeaf) throw new UnsupportedOperationException("value cannot be obtained from a non-leaf node");

        int idx = getKeyIndex(key);
        assert idx >= 0;

        if (this.keys.get(idx).compareTo(key) != 0)
            return null;

        return this.values.get(idx);
    }

    public int getChildRef(Key key) {
        if (this.isLeaf) throw new UnsupportedOperationException("childRef cannot be obtained from a leaf node");


        int idx = getKeyIndex(key);
        assert idx >= 0;

        return this.childrenRefs.get(idx);
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public List<Key> getKeys() {
        return Collections.unmodifiableList(this.keys);
    }

    public List<Value> getValues() {
        return Collections.unmodifiableList(this.values);
    }

    public List<Integer> getChildrenRefs() {
        return Collections.unmodifiableList(this.childrenRefs);
    }

    //region Node insertions and updates
    public void nodeUpdate(Key key, int child) {
        nodeUpdate(key, child, null);
    }

    public void nodeUpdate(Key key, int child, Key newKey) {
        if (this.isLeaf) throw new UnsupportedOperationException("child ref cannot inserted to leaf node");

        if (this.keys.isEmpty()) {
            this.keys.add(key);
            this.childrenRefs.add(child);
        }

        int idx = getKeyIndex(key);
        assert idx >= 0;

        if (newKey != null) {
            this.keys.set(idx, newKey);
            this.childrenRefs.set(idx, child);
            return;
        }
        if (this.keys.get(idx).compareTo(key) == 0)
            this.childrenRefs.set(idx, child);
        else {
            this.keys.add(idx + 1, key);
            this.childrenRefs.add(idx + 1, child);
        }
    }

    public void leafUpdate(Key key, Value value) {
        if (!this.isLeaf) throw new UnsupportedOperationException("value cannot be inserted to a non-leaf node");

        if (this.keys.isEmpty()) {
            this.keys.add(key);
            this.values.add(value);
            return;
        }

        int idx = getKeyIndex(key);
        assert idx >= 0;

        if (this.keys.get(idx).compareTo(key) == 0)
            this.values.set(idx, value);
        else {
            this.keys.add(idx + 1, key);
            this.values.add(idx + 1, value);
        }
    }
    //endregion

    //region Node deletion
    public boolean leafDelete(Key key) {
        if (!this.isLeaf) throw new UnsupportedOperationException("value cannot be deleted from a non-leaf node");

        if (this.keys.isEmpty())
            return false;

        int idx = getKeyIndex(key);
        assert idx >= 0;

        if (this.keys.get(idx).compareTo(key) == 0) {
            this.keys.remove(idx);
            this.values.remove(idx);
            return true;
        }
        return false;
    }

    public boolean nodeDelete(Key key) {
        if (this.isLeaf) throw new UnsupportedOperationException("childrenRef cannot be deleted from a non-leaf node");

        if (this.keys.isEmpty())
            return false;

        int idx = getKeyIndex(key);
        assert idx >= 0;

        this.keys.remove(idx);
        this.childrenRefs.remove(idx);
        return true;
    }
    //endregion

    //region Work with children

    public int getRightSiblingRef(Key key) {
        if (this.isLeaf) throw new UnsupportedOperationException("sibling cannot be obtained from a leaf node");

        int idx = getKeyIndex(key);
        assert idx >= 0;

        if ((idx + 1) >= this.childrenRefs.size())
            return -1;

        return this.childrenRefs.get(idx + 1);
    }

    public int getLeftSiblingRef(Key key) {
        if (this.isLeaf) throw new UnsupportedOperationException("sibling cannot be obtained from a leaf node");

        int idx = getKeyIndex(key);
        assert idx >= 0;

        if (idx < 1)
            return -1;

        return this.childrenRefs.get(idx - 1);
    }

    public void insertSplitChildren(List<Node> children, List<Integer> refs) {
        for (int i = 0; i < children.size(); i++) {
            nodeUpdate(children.get(i).keys.getFirst(), refs.get(i));
        }
    }

    public Node mergeTwoChildren(Node left, Node right) {
        assert left.isLeaf == right.isLeaf : "Incompatible children types";

        left.keys.addAll(right.keys);
        if (left.isLeaf) {
            left.values.addAll(right.values);
        } else {
            left.childrenRefs.addAll(right.childrenRefs);
        }

        return left;
    }
    //endregion

    //region Node splitting
    public static List<Node> split(Node old) {
        if (old.nodeSize() <= PagingConstants.PAGE_SIZE) {
            return Collections.singletonList(old);
        }

        Node[] two = split2(old);
        Node left = two[0], right = two[1];

        if (left.nodeSize() <= PagingConstants.PAGE_SIZE) {
            return List.of(left, right);
        }

        Node[] leftSplit = split2(left);
        return List.of(leftSplit[0], leftSplit[1], right);
    }

    private static Node[] split2(Node old) {
        int nkeys = old.keys.size();
        if (nkeys < 2) throw new IllegalArgumentException("need >=2 keys to split");

        int nleft = nkeys / 2;

        while (nleft > 0 && partialSize(old, 0, nleft) > PagingConstants.PAGE_SIZE) {
            nleft--;
        }
        if (nleft < 1) nleft = 1;

        while (nleft < nkeys && partialSize(old, nleft, nkeys) > PagingConstants.PAGE_SIZE) {
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
            size += 2 + src.keys.get(i).getKeyLength();
        }
        if (src.isLeaf) {
            for (int i = from; i < to; i++) {
                size += 2 + src.values.get(i).value().length;
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

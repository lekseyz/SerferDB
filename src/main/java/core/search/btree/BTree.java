package core.search.btree;

import core.page.PageDumper;
import core.page.PagingConstants;
import core.search.Key;
import core.search.Searcher;
import core.search.Value;

import java.util.AbstractMap;
import java.util.List;

public class BTree implements Searcher {
    private int root;
    private final PageDumper dumper;

    public BTree(PageDumper dumper) {
        this.dumper = dumper;
        root = -1;
    }

    @Override
    public Value search(Key key) {
        if (root == -1)
            return null;

        return search(key, Node.decode(dumper.get(root)));
    }

    private Value search(Key key, Node curNode) {
        if (curNode.isLeaf()) {
            return curNode.getKeyValue(key);
        }

        int ref = curNode.getChildRef(key);
        Node nextNode = Node.decode(dumper.get(ref));
        return search(key, nextNode);
    }

    @Override
    public void insert(Key key, Value value) {
        if (root == -1) {
            Node node = new Node(true);
            node.leafUpdate(Key.NullKey(), Value.NullValue()); // Manually inserting minimal possible value
            node.leafUpdate(key, value);
            root = dumper.set(Node.encode(node));
            return;
        }
        Node curRoot = Node.decode(dumper.get(this.root));
        dumper.delete(this.root);
        Node node = insert(key, value, curRoot);
        List<Node> split = Node.split(node);
        List<Integer> splitRefs;
        splitRefs = split.stream().map(s -> dumper.set(Node.encode(s))).toList();

        if (split.size() > 1) {
            Node newRoot = new Node(false);
            newRoot.insertSplitChildren(split, splitRefs);
            root = dumper.set(Node.encode(newRoot));
        } else {
            root = splitRefs.getFirst();
        }
    }

    private Node insert(Key key, Value value, Node curNode) {
        if (curNode.isLeaf()) {
            curNode.leafUpdate(key, value);
            return curNode;
        }
        int nodeRef = curNode.getChildRef(key);
        Node node = Node.decode(dumper.get(nodeRef));
        dumper.delete(nodeRef);
        node = insert(key, value, node);

        var split = Node.split(node);
        var splitRefs = split.stream().map(s -> dumper.set(Node.encode(s))).toList();
        curNode.insertSplitChildren(split, splitRefs);
        return curNode;
    }

    @Override
    public boolean delete(Key key) {
        if (root == -1) return false;

        Node node = Node.decode(dumper.get(root));
        node = delete(key, node);
        if (node == null) return false;

        dumper.delete(root);

        if (node.getKeys().size() < 2) {
            if (node.isLeaf())
                root = -1;
            else
                root = node.getChildrenRefs().getFirst();
        } else {
            root = dumper.set(Node.encode(node));
        }
        return true;
    }

    public Node delete(Key key, Node node) {

        if (node.isLeaf()) {
            if (!node.leafDelete(key))
                return null;
            return node;
        }
        int ref = node.getChildRef(key);
        Node child = Node.decode(dumper.get(ref));
        child = delete(key, child);

        if (child == null) return null;

        dumper.delete(ref);
        var entry = shouldMerge(node, child, key);
        int mergeDir = entry.getKey();
        Node sibling = entry.getValue();

        if (mergeDir < 0) {
            assert sibling != null;
            node.nodeDelete(key);
            Node merged = node.mergeTwoChildren(sibling, child);
            node.nodeUpdate(merged.getKeys().getFirst(), dumper.set(Node.encode(merged)), merged.getKeys().getFirst());
            return node;
        } else if (mergeDir > 0) {
            assert sibling != null;
            node.nodeDelete(sibling.getKeys().getFirst());
            Node merged = node.mergeTwoChildren(child, sibling);
            node.nodeUpdate(key, dumper.set(Node.encode(merged)), merged.getKeys().getFirst());
            return node;
        }

        if (child.getKeys().isEmpty()) {
            return node;
        }
        node.nodeUpdate(key, dumper.set(Node.encode(child)), child.getKeys().getFirst()); //здесь
        return node;
    }

    private AbstractMap.SimpleEntry<Integer, Node> shouldMerge(Node parent, Node child, Key key) {
        if (!child.isMergingSize()) {
            return new AbstractMap.SimpleEntry<>(0, null);
        }

        int leftSibling = parent.getLeftSiblingRef(key);
        int rightSibling = parent.getRightSiblingRef(key);
        if (leftSibling != -1) {
            Node sibling = Node.decode(dumper.get(leftSibling));
            if (sibling.nodeSize() + child.nodeSize() <= PagingConstants.MAX_PAGE_SIZE) {
                dumper.delete(leftSibling);
                return new AbstractMap.SimpleEntry<>(-1, sibling);
            }
        }
        if (rightSibling != -1) {
            Node sibling = Node.decode(dumper.get(rightSibling));
            if (sibling.nodeSize() + child.nodeSize() <= PagingConstants.MAX_PAGE_SIZE) {
                dumper.delete(rightSibling);
                return new AbstractMap.SimpleEntry<>(1, sibling);
            }
        }

        return new AbstractMap.SimpleEntry<>(0, null);
    }
}

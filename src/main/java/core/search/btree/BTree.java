package core.search.btree;

import core.exception.StorageAccessException;
import core.page.PageDumper;
import core.page.PagingConstants;
import core.search.Key;
import core.search.Searcher;
import core.search.Value;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;

import static core.page.PagingConstants.UNDEFINED_REF;

public class BTree implements Searcher {
    private int root;
    private final PageDumper dumper;

    public BTree(PageDumper dumper) {
        this.dumper = dumper;
        try {
            root = this.dumper.getRoot();
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public Value search(Key key) {
        if (root == UNDEFINED_REF)
            return null;

        try {
            return search(key, Node.decode(dumper.get(root)));
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }
    }

    private Value search(Key key, Node curNode) {
        if (curNode.isLeaf()) {
            return curNode.getKeyValue(key);
        }

        int ref = curNode.getChildRef(key);
        Node nextNode = null;
        try {
            nextNode = Node.decode(dumper.get(ref));
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }
        return search(key, nextNode);
    }

    @Override
    public void insert(Key key, Value value) {
        if (root == UNDEFINED_REF) {
            Node node = new Node(true);
            node.leafUpdate(Key.NullKey(), Value.NullValue()); // Manually inserting minimal possible key
            node.leafUpdate(key, value);
            try {
                root = dumper.set(Node.encode(node));
            } catch (IOException e) {
                throw new StorageAccessException(e.getMessage(), e.getCause());
            }
            try {
                dumper.setRoot(root);
            } catch (IOException e) {
                throw new StorageAccessException(e.getMessage(), e.getCause());
            }
            return;
        }
        Node curRoot = null;
        try {
            curRoot = Node.decode(dumper.get(this.root));
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }
        try {
            dumper.delete(this.root);
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }
        Node node = insert(key, value, curRoot);
        List<Node> split = Node.split(node);
        List<Integer> splitRefs;
        splitRefs = split.stream().map(s -> {
            try {
                return dumper.set(Node.encode(s));
            } catch (IOException e) {
                throw new StorageAccessException(e.getMessage(), e.getCause());
            }
        }).toList();

        if (split.size() > 1) {
            Node newRoot = new Node(false);
            newRoot.insertSplitChildren(split, splitRefs);
            try {
                root = dumper.set(Node.encode(newRoot));
            } catch (IOException e) {
                throw new StorageAccessException(e.getMessage(), e.getCause());
            }
        } else {
            root = splitRefs.getFirst();
        }
        try {
            dumper.setRoot(root);
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }
    }

    private Node insert(Key key, Value value, Node curNode) {
        if (curNode.isLeaf()) {
            curNode.leafUpdate(key, value);
            return curNode;
        }
        int nodeRef = curNode.getChildRef(key);
        Node node = null;
        try {
            node = Node.decode(dumper.get(nodeRef));
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }
        try {
            dumper.delete(nodeRef);
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }
        node = insert(key, value, node);

        var split = Node.split(node);
        var splitRefs = split.stream().map(s -> {
            try {
                return dumper.set(Node.encode(s));
            } catch (IOException e) {
                throw new StorageAccessException(e.getMessage(), e.getCause());
            }
        }).toList();
        curNode.insertSplitChildren(split, splitRefs);
        return curNode;
    }

    @Override
    public boolean delete(Key key) {
        if (root == -1) return false;

        Node node = null;
        try {
            node = Node.decode(dumper.get(root));
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }
        node = delete(key, node);
        if (node == null) return false;

        try {
            dumper.delete(root);
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }

        if (node.getKeys().size() < 2) {
            if (node.isLeaf())
                root = UNDEFINED_REF;
            else
                root = node.getChildrenRefs().getFirst();
        } else {
            try {
                root = dumper.set(Node.encode(node));
            } catch (IOException e) {
                throw new StorageAccessException(e.getMessage(), e.getCause());
            }
        }
        try {
            dumper.setRoot(root);
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
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
        Node child = null;
        try {
            child = Node.decode(dumper.get(ref));
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }
        child = delete(key, child);

        if (child == null) return null;

        try {
            dumper.delete(ref);
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }
        var entry = shouldMerge(node, child, key);
        int mergeDir = entry.getKey();
        Node sibling = entry.getValue();

        if (mergeDir < 0) {
            assert sibling != null;
            node.nodeDelete(key);
            Node merged = node.mergeTwoChildren(sibling, child);
            try {
                node.nodeUpdate(merged.getKeys().getFirst(), dumper.set(Node.encode(merged)), merged.getKeys().getFirst());
            } catch (IOException e) {
                throw new StorageAccessException(e.getMessage(), e.getCause());
            }
            return node;
        } else if (mergeDir > 0) {
            assert sibling != null;
            node.nodeDelete(sibling.getKeys().getFirst());
            Node merged = node.mergeTwoChildren(child, sibling);
            try {
                node.nodeUpdate(key, dumper.set(Node.encode(merged)), merged.getKeys().getFirst());
            } catch (IOException e) {
                throw new StorageAccessException(e.getMessage(), e.getCause());
            }
            return node;
        }

        if (child.getKeys().isEmpty()) {
            return node;
        }
        try {
            node.nodeUpdate(key, dumper.set(Node.encode(child)), child.getKeys().getFirst()); //здесь
        } catch (IOException e) {
            throw new StorageAccessException(e.getMessage(), e.getCause());
        }
        return node;
    }

    private AbstractMap.SimpleEntry<Integer, Node> shouldMerge(Node parent, Node child, Key key) {
        if (!child.isMergingSize()) {
            return new AbstractMap.SimpleEntry<>(0, null);
        }

        int leftSibling = parent.getLeftSiblingRef(key);
        int rightSibling = parent.getRightSiblingRef(key);
        if (leftSibling != -1) {
            Node sibling = null;
            try {
                sibling = Node.decode(dumper.get(leftSibling));
            } catch (IOException e) {
                throw new StorageAccessException(e.getMessage(), e.getCause());
            }
            if (sibling.nodeSize() + child.nodeSize() <= PagingConstants.PAGE_SIZE) {
                try {
                    dumper.delete(leftSibling);
                } catch (IOException e) {
                    throw new StorageAccessException(e.getMessage(), e.getCause());
                }
                return new AbstractMap.SimpleEntry<>(-1, sibling);
            }
        }
        if (rightSibling != -1) {
            Node sibling = null;
            try {
                sibling = Node.decode(dumper.get(rightSibling));
            } catch (IOException e) {
                throw new StorageAccessException(e.getMessage(), e.getCause());
            }
            if (sibling.nodeSize() + child.nodeSize() <= PagingConstants.PAGE_SIZE) {
                try {
                    dumper.delete(rightSibling);
                } catch (IOException e) {
                    throw new StorageAccessException(e.getMessage(), e.getCause());
                }
                return new AbstractMap.SimpleEntry<>(1, sibling);
            }
        }

        return new AbstractMap.SimpleEntry<>(0, null);
    }
}

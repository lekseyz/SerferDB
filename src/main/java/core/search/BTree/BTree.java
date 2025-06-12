package core.search.BTree;

public class BTree {
    private int root;

    private Node get (int id) {
        return null;
    }

    private int set(Node node) {
        return 0;
    }

    private void deletec(int ref) {

    }

    public void insert(Key key, Value value) {
        if (root == -1) {
            Node node = new Node(true);
            node.leafUpdate(Key.NullKey(), Value.NullValue()); // Manually inserting minimal possible value
            node.leafUpdate(key, value);
            root = set(node);
            return;
        }
        Node curRoot = get(this.root);
        deletec(this.root);
        Node node = insert(key, value, get(root));
        var split = Node.split(node);
        var splitRefs = split.stream().map(this::set).toList();
        if (split.size() > 1) {
            Node newRoot = new Node(false);
            newRoot.insertSplitChildren(split, splitRefs);
            root = set(newRoot);
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
        Node node = get(nodeRef);
        deletec(nodeRef);
        node = insert(key, value, node);

        var split = Node.split(node);
        var splitRefs = split.stream().map(this::set).toList();
        curNode.insertSplitChildren(split, splitRefs);
        return curNode;
    }

    public boolean delete(Key key) {
        if (root == -1) return false;

        Node node = get(root);
        node = delete(key, node);
        if (node == null) return false;

        deletec(root);

        if (node.getKeys().size() < 2) {
            root = node.getChildrenRefs().getFirst();
        } else {
            root = set(node);
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
        Node child = get(ref);
        child = delete(key, child);

        if (child == null) return null;

        deletec(ref);
        Node sibling = null;
        int mergeDir = shouldMerge(node, child, sibling);

        if (mergeDir < 0) {
            assert sibling != null;
            Node merged = node.mergeTwoChildren(sibling, child);
            node.nodeUpdate(merged.getKeys().getFirst(), set(merged), merged.getKeys().getFirst());
            return node;
        } else if (mergeDir > 0) {
            assert sibling != null;
            Node merged = node.mergeTwoChildren(child, sibling);
            node.nodeUpdate(merged.getKeys().getFirst(), set(merged), merged.getKeys().getFirst());
            return node;
        }

        if (child.getKeys().isEmpty()) {
            node.nodeDelete(key);
            return node;
        }
        node.nodeUpdate(key, set(child), child.getKeys().getFirst());
        return node;
    }

    private int shouldMerge(Node parent, Node child, Node outSibling) {
        if (!child.isMergingSize()) {
            outSibling = null;
            return 0;
        }

        int leftSibling = parent.getLeftSiblingRef(child.getKeys().getFirst());
        int rightSibling = parent.getRightSiblingRef(child.getKeys().getFirst());
        if (leftSibling != -1) {
            Node sibling = get(leftSibling);
            if (sibling.nodeSize() + child.nodeSize() <= Node.PAGE_SIZE) {
                outSibling = sibling;
                return -1;
            }
        }
        if (rightSibling != -1) {
            Node sibling = get(rightSibling);
            if (sibling.nodeSize() + child.nodeSize() <= Node.PAGE_SIZE) {
                outSibling = sibling;
                return 1;
            }
        }

        outSibling = null;
        return 0;
    }
}

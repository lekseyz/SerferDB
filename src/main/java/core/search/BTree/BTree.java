package core.search.BTree;

public class BTree {
    private int root;

    private Node get (int id) {
        return null;
    }

    private int set(Node node) {
        return 0;
    }

    private void delete(int ref) {

    }

    public void Insert(Node.Key key, Node.Value value) {
        if (root == -1) {
            Node node = new Node(true);
            node.LeafUpdate(Node.Key.NullKey(), Node.Value.NullValue()); // Manually inserting minimal possible value
            node.LeafUpdate(key, value);
            root = set(node);
            return;
        }
        Node curRoot = get(this.root);
        delete(this.root);
        Node node = Insert(key, value, get(root));
        var split = Node.split(node);
        var splitRefs = split.stream().map(this::set).toList();
        if (split.size() > 1) {
            Node newRoot = new Node(false);
            newRoot.InsertSplitChildren(split, splitRefs);
            root = set(newRoot);
        } else {
            root = splitRefs.getFirst();
        }
    }

    private Node Insert(Node.Key key, Node.Value value, Node curNode) {
        if (curNode.isLeaf) {
            curNode.LeafUpdate(key, value);
            return curNode;
        }
        int nodeRef = curNode.GetChildRef(key);
        Node node = get(nodeRef);
        delete(nodeRef);
        node = Insert(key, value, node);

        var split = Node.split(node);
        var splitRefs = split.stream().map(this::set).toList();
        curNode.InsertSplitChildren(split, splitRefs);
        return curNode;
    }

    public boolean Delete(Node.Key key) {
        if (root == -1) return false;

        Node node = get(root);
        node = Delete(key, node);
        if (node == null) return false;

        delete(root);

        if (node.keys.size() < 2) {
            root = node.childrenRefs.getFirst();
        } else {
            root = set(node);
        }
        return true;
    }

    public Node Delete(Node.Key key, Node node) {

        if (node.isLeaf) {
            if (!node.LeafDelete(key))
                return null;
            return node;
        }
        int ref = node.GetChildRef(key);
        Node child = get(ref);
        child = Delete(key, child);

        if (child == null) return null;

        delete(ref);
        Node sibling = null;
        int mergeDir = ShouldMerge(node, child, sibling);

        if (mergeDir < 0) {
            Node merged = node.MergeTwoChildren(sibling, child);
            node.NodeUpdate(merged.keys.getFirst(), set(merged), merged.keys.getFirst());
            return node;
        } else if (mergeDir > 0) {
            Node merged = node.MergeTwoChildren(child, sibling);
            node.NodeUpdate(merged.keys.getFirst(), set(merged), merged.keys.getFirst());
            return node;
        }

        if (child.keys.isEmpty()) {
            node.NodeDelete(key);
            return node;
        }
        node.NodeUpdate(key, set(child), child.keys.getFirst());
        return node;
    }

    private int ShouldMerge(Node parent, Node child, Node outSibling) {
        if (child.NodeSize() > Node.PAGE_SIZE / 4) {
            outSibling = null;
            return 0;
        }

        int idx = parent.GetKeyIndex(child.keys.getFirst());
        if (idx > 0) {
            Node sibling = get(parent.childrenRefs.get(idx - 1));
            if (sibling.NodeSize() + child.NodeSize() <= Node.PAGE_SIZE) {
                outSibling = sibling;
                return -1;
            }
        }
        if (parent.keys.size() > (idx + 1)) {
            Node sibling = get(parent.childrenRefs.get(idx + 1));
            if (sibling.NodeSize() + child.NodeSize() <= Node.PAGE_SIZE) {
                outSibling = sibling;
                return 1;
            }
        }

        outSibling = null;
        return 0;
    }
}

package core.search.BTree;

public class Value {
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
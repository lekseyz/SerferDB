package core.search;

public class Value {
    public static final int MAX_VALUE_SIZE = 512;
    public byte[] value;

    public Value(byte[] value) {
        if (value.length > MAX_VALUE_SIZE)
            throw new IllegalArgumentException("value overflow");
        this.value = value;
    }

    public Value() {
        this.value = null;
    }

    public static Value NullValue() {
        return new Value(new byte[]{0});
    }
}
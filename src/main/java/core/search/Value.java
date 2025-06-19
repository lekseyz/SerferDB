package core.search;

import java.util.Arrays;

public record Value(byte[] value) {
    public static final int MAX_VALUE_SIZE = 512;

    public Value(byte[] value) {
        if (value.length > MAX_VALUE_SIZE)
            throw new IllegalArgumentException("value overflow");
        this.value = Arrays.copyOf(value, value.length);
    }

    @Override
    public byte[] value() {
        return this.value;
    }

    public static Value NullValue() {
        return new Value(new byte[0]);
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj) ||
                (obj instanceof Value(byte[] value1) && Arrays.equals(value, value1));
    }

    @Override
    public String toString() {
        return Arrays.toString(this.value);
    }
}
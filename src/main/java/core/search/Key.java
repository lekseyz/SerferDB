package core.search;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public record Key(byte[] key) implements Comparable<Key> {
    public static final int MAX_KEY_SIZE = 256;

    public Key(byte[] key) {
        if (key.length > MAX_KEY_SIZE)
            throw new IllegalArgumentException("Key overflow");
        this.key = Arrays.copyOf(key, key.length);
    }

    public static Key NullKey() {
        return new Key(new byte[]{0});
    }

    public static Key from(String key) {
        var bytes = key.getBytes(Charset.defaultCharset());
        return new Key(ByteBuffer
                .allocate(bytes.length + 1)
                .put(bytes)
                .put((byte)1)
                .array());
    }

    @Override
    public byte[] key() {
        return this.key;
    }

    public Key getCopy() {
        return new Key(this.key());
    }

    public int getKeyLength() {
        return key.length;
    }

    @Override
    public int compareTo(Key o) {
        int min = Math.min(key.length, o.key.length);

        for (int i = 0; i < min; i++) {
            int cmp = Byte.compareUnsigned(key[i], o.key[i]);
            if (cmp != 0) return cmp;
        }
        return Integer.compare(key.length, o.key.length);
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj) ||
                (obj instanceof Key(byte[] key1) && Arrays.equals(key, key1));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }

    @Override
    public String toString() {
        return Arrays.toString(key);
    }
}

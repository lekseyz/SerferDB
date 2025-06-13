package core.search;

public class Key implements Comparable<Key>{
    public static final int MAX_KEY_SIZE = 256;
    public byte[] key;

    public Key(byte[] key) {
        if (key.length > MAX_KEY_SIZE)
            throw new IllegalArgumentException("key overflow");
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

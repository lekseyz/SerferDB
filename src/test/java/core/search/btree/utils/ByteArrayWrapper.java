package core.search.btree.utils;

import core.search.Key;
import core.search.Value;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public class ByteArrayWrapper {
    public static byte[] toBytes(int value) {
        return ByteBuffer.allocate(5).putInt(value).put((byte)1).array();
    }

    public static byte[] toBytes(String value) {
        if (value == null) throw new IllegalArgumentException("Value cannot be null");

        var bytes = value.getBytes(Charset.defaultCharset());
        return ByteBuffer.allocate(bytes.length + 1).put(bytes).put((byte)1).array();
    }

    public static String getString(byte[] bytes) {
        if (bytes.length < 2) throw new IllegalArgumentException("To short byte array, cannot get string");

        return new String(Arrays.copyOf(bytes, bytes.length - 1));
    }

    public static int getInt(byte[] bytes) {
        if (bytes.length < 5) throw new IllegalArgumentException("To short byte array, cannot get int");
        var buff = ByteBuffer.wrap(bytes);
        return buff.getInt();
    }

    public static Key getKey(int value) {
        return new Key(toBytes(value));
    }

    public static Key getKey(String value) {
        return new Key(toBytes(value));
    }

    public static Value getValue(int value) {
        return new Value(toBytes(value));
    }

    public static Value getValue(String value) {
        return new Value(toBytes(value));
    }

    private ByteArrayWrapper() {

    }
}

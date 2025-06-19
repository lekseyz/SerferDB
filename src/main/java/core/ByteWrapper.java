package core;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ByteWrapper {

    public static byte[] fromInt(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value
        };
    }

    public static int toInt(byte[] bytes) {
        if (bytes.length < 4)
            throw new IllegalArgumentException("Invalid byte array for int");

        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8)  |
                (bytes[3] & 0xFF);
    }

    public static byte[] fromLong(long value) {
        byte[] res = new byte[8];
        for (int i = 7; i >= 0; i--) {
            res[i] = (byte)(value & 0xFF);
            value >>= 8;
        }
        return res;
    }

    public static long toLong(byte[] bytes) {
        if (bytes.length < 8)
            throw new IllegalArgumentException("Invalid byte array for long");

        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (bytes[i] & 0xFF);
        }
        return result;
    }

    public static byte[] fromShort(short value) {
        return new byte[] {
                (byte)(value >>> 8),
                (byte)value
        };
    }

    public static short toShort(byte[] bytes) {
        if (bytes.length < 2)
            throw new IllegalArgumentException("Invalid byte array for short");

        return (short)(((bytes[0] & 0xFF) << 8) |
                (bytes[1] & 0xFF));
    }

    public static byte[] fromBoolean(boolean value) {
        return new byte[] { (byte)(value ? 1 : 0) };
    }

    public static boolean toBoolean(byte[] bytes) {
        if (bytes.length < 1)
            throw new IllegalArgumentException("Invalid byte array for boolean");

        return bytes[0] != 0;
    }

    public static byte[] fromString(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    public static String toString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] clone(byte[] src) {
        return Arrays.copyOf(src, src.length);
    }
}

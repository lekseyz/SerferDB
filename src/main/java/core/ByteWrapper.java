package core;

public class ByteWrapper {
    public static byte[] fromString(String value) {
        return value.getBytes();
    }

    public static byte[] fromInt(int value) {
        return new byte[]{
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }

    public static int toInt(byte[] bytes) {
        if (bytes.length < 4) throw new IllegalArgumentException();
        return ((int)bytes[0] << 24) + ((int)bytes[1] << 16)
                + ((int)bytes[2] << 8) + bytes[3];
    }

    public static String toString(byte[] bytes) {
        return new String(bytes);
    }
}

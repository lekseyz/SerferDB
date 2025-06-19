package api;

import core.ByteWrapper;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

public class SEntity {
    private final byte[] content;
    private final STypes type;

    private SEntity(byte[] buffer, STypes type) {
        this.content = Arrays.copyOf(buffer, buffer.length);
        this.type = type;
    }

    public static SEntity of(String value) {
        return new SEntity(ByteWrapper.fromString(value), STypes.String);
    }

    public static SEntity of(int value) {
        return new SEntity(ByteWrapper.fromInt(value), STypes.Int);
    }

    public Optional<Integer> asInt() {
        if (type != STypes.Int)
            return Optional.empty();
        try {
            return Optional.of(ByteWrapper.toInt(content));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Optional<String> asString() {
        if (type != STypes.String)
            return Optional.empty();

        return Optional.of(ByteWrapper.toString(content));
    }

    public STypes getType() {
        return type;
    }

    public static byte[] serialize(SEntity entity) {
        var buffer = ByteBuffer.allocate(entity.content.length + 1);
        buffer.put(entity.type == STypes.Int ? (byte)1 : (byte)2);
        buffer.put(entity.content);

        return buffer.array();
    }

    public static SEntity deserialize(byte[] bytes) {
        var buffer = ByteBuffer.wrap(bytes);
        STypes type = buffer.get() == 1 ? STypes.Int : STypes.String;
        var content = new byte[buffer.remaining()];
        buffer.get(content);

        return new SEntity(content, type);
    }
}

package api;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TestSEntity {

    @Test
    void testOfInt() {
        SEntity entity = SEntity.of(123456);
        assertEquals(STypes.Int, entity.getType());

        Optional<Integer> value = entity.asInt();
        assertTrue(value.isPresent());
        assertEquals(123456, value.get());

        assertTrue(entity.asString().isEmpty());
    }

    @Test
    void testOfString() {
        String original = "hello мир";
        SEntity entity = SEntity.of(original);
        assertEquals(STypes.String, entity.getType());

        Optional<String> value = entity.asString();
        assertTrue(value.isPresent());
        assertEquals(original, value.get());

        assertTrue(entity.asInt().isEmpty());
    }

    @Test
    void testSerializationInt() {
        SEntity original = SEntity.of(42);
        byte[] serialized = SEntity.serialize(original);

        SEntity restored = SEntity.deserialize(serialized);
        assertEquals(STypes.Int, restored.getType());
        assertEquals(original.asInt().get(), restored.asInt().get());
    }

    @Test
    void testSerializationString() {
        SEntity original = SEntity.of("abc123");
        byte[] serialized = SEntity.serialize(original);

        SEntity restored = SEntity.deserialize(serialized);
        assertEquals(STypes.String, restored.getType());
        assertEquals(original.asString().get(), restored.asString().get());
    }

    @Test
    void testDifferentEntitiesNotEqual() {
        SEntity intEntity = SEntity.of(1);
        SEntity strEntity = SEntity.of("1");

        assertNotEquals(intEntity.getType(), strEntity.getType());
        assertNotEquals(intEntity.asString(), strEntity.asString());
    }

    @Test
    void testEmptyContentDeserialization() {
        byte[] data = SEntity.serialize(SEntity.of(""));
        SEntity entity = SEntity.deserialize(data);

        assertEquals(STypes.String, entity.getType());
        assertEquals("", entity.asString().orElse("not empty"));
    }

    @Test
    void testNonUtf8ContentHandledGracefully() {
        byte[] bytes = new byte[] { 2, (byte)0xFF, (byte)0xFE, (byte)0xFD };
        SEntity entity = SEntity.deserialize(bytes);
        assertEquals(STypes.String, entity.getType());

        assertTrue(entity.asString().isPresent());
    }

    @Test
    void testInvalidDeserializationGracefullyFails() {
        byte[] bytes = new byte[] { 1 };
        SEntity entity = SEntity.deserialize(bytes);
        assertEquals(STypes.Int, entity.getType());

        assertTrue(entity.asInt().isEmpty());
    }
}

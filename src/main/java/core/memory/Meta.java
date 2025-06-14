package core.memory;

import core.page.Page;
import core.page.PagingConstants;

import java.nio.ByteBuffer;

import static core.page.PagingConstants.UNDEFINED_REF;

public class Meta implements Page {
    public int rootRef;
    public int freeListRef;

    public Meta(int rootRef) {
        this.rootRef = rootRef;
        freeListRef = UNDEFINED_REF;
    }

    public Meta(int rootRef, int freeListRef) {
        this.rootRef = rootRef;
        this.freeListRef = freeListRef;
    }

    public static ByteBuffer encode(Meta page) {
        ByteBuffer buffer = ByteBuffer.allocate(PagingConstants.PAGE_SIZE);
        buffer.putInt(page.rootRef);
        buffer.putInt(page.freeListRef);
        return buffer.flip();
    }

    public static Meta decode(ByteBuffer buffer) {
        if (buffer.capacity() < PagingConstants.PAGE_SIZE) throw new RuntimeException("error decoding meta page");
        var meta = new Meta(buffer.getInt());
        meta.freeListRef = buffer.getInt();
        return meta;
    }
}

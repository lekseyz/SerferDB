package core.memory;

import core.page.Page;
import core.page.PagingConstants;

import java.io.InvalidObjectException;
import java.nio.ByteBuffer;

import static core.page.PagingConstants.UNDEFINED_REF;

public class Meta implements Page {
    public int rootRef;
    public int freeListRef;
    public int nextNodeIdx;

    public Meta(int rootRef) {
        this.rootRef = rootRef;
        freeListRef = UNDEFINED_REF;
        nextNodeIdx = 1;
    }

    public Meta(int rootRef, int freeListRef) {
        this.rootRef = rootRef;
        this.freeListRef = freeListRef;
        nextNodeIdx = 1;
    }

    public Meta(int rootRef, int freeListRef, int  nextNodeIdx) {
        this.rootRef = rootRef;
        this.freeListRef = freeListRef;
        this.nextNodeIdx = nextNodeIdx;
    }

    public static ByteBuffer encode(Meta page) {
        ByteBuffer buffer = ByteBuffer.allocate(PagingConstants.PAGE_SIZE);
        buffer.putInt(page.rootRef);
        buffer.putInt(page.freeListRef);
        buffer.putInt(page.nextNodeIdx);
        return buffer.flip();
    }

    public static Meta decode(ByteBuffer buffer) throws InvalidObjectException {
        if (buffer.capacity() < PagingConstants.PAGE_SIZE) throw new RuntimeException("Error decoding meta page");
        var meta = new Meta(buffer.getInt());
        meta.freeListRef = buffer.getInt();
        meta.nextNodeIdx = buffer.getInt();
        return meta;
    }
}

package core.memory;

import core.page.Page;
import core.page.PagingConstants;

import java.nio.ByteBuffer;

public class FreeList implements Page {
    public int nextRef;

    public static ByteBuffer encode(FreeList listPage) {
        var buffer = ByteBuffer.allocate(PagingConstants.PAGE_SIZE);
        buffer.putInt(listPage.nextRef);

        return buffer;
    }

    public static FreeList decode(ByteBuffer buffer) {
        var listPage = new FreeList();
        listPage.nextRef = buffer.getInt();

        return listPage;
    }
}

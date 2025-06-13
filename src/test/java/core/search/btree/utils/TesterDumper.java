package core.search.btree.utils;

import core.page.PageDumper;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import static core.page.PagingConstants.MAX_PAGE_SIZE;

public class TesterDumper implements PageDumper {
    public final Map<Integer, ByteBuffer> pages;
    public int nextIndex;

    public TesterDumper() {
        pages = new TreeMap<>();
        nextIndex = 0;
    }

    @Override
    public ByteBuffer get(int idx) {
        if (pages.isEmpty()) throw new RuntimeException("trying to get page from empty dump");
        if (!pages.containsKey(idx)) throw new RuntimeException("trying to get non-allocated or freed page");

        return pages.get(idx);
    }

    @Override
    public int set(ByteBuffer bytes) {
        if (bytes.capacity() > MAX_PAGE_SIZE) throw new RuntimeException("byte buffer overflow");

        int newIdx = nextIndex++;

        pages.put(newIdx, bytes);
        return newIdx;
    }

    @Override
    public void delete(int idx) {
        if (pages.isEmpty()) throw new RuntimeException("trying to delete page from empty dump");
        if (!pages.containsKey(idx)) throw new RuntimeException("trying to delete non-allocated or freed page");

        pages.remove(idx);
    }
}

package core.memory;

import core.page.PageDumper;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import static core.page.PagingConstants.PAGE_SIZE;
import static core.page.PagingConstants.UNDEFINED_REF;

public class DiskPageDumper implements PageDumper {
    private final RandomAccessFile dataFile;
    private final RandomAccessFile walFile;
    private int nextPageId = 1;
    private Meta meta;

    public DiskPageDumper(Path dataPath, Path walPath) throws IOException {
        dataFile = new RandomAccessFile(dataPath.toFile(), "rw");
        meta = null;
        walFile = new RandomAccessFile(walPath.toFile(), "rw");
        recoverFromWAL();
        initializeIfEmpty();
    }

    @Override
    public synchronized ByteBuffer get(int idx) {
        try {
            byte[] buffer = new byte[PAGE_SIZE];
            dataFile.seek((long) (idx + 1) * PAGE_SIZE);
            dataFile.readFully(buffer);
            return ByteBuffer.wrap(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized int set(ByteBuffer bytes) {
        try {
            int ref = UNDEFINED_REF;
            if (meta == null) readMeta();
            if (meta.freeListRef != UNDEFINED_REF) {
                ref = meta.freeListRef;
                var buffer = new byte[PAGE_SIZE];
                dataFile.seek((long) (meta.freeListRef + 1) * PAGE_SIZE);
                dataFile.readFully(buffer);
                var listPage = FreeList.decode(ByteBuffer.wrap(buffer));
                meta.freeListRef = listPage.nextRef;
                writeMeta();
            } else
                ref = nextPageId++;

            byte[] data = new byte[PAGE_SIZE];
            bytes.get(data);

            walFile.seek(walFile.length());
            walFile.writeInt(ref);
            walFile.writeInt(PAGE_SIZE);
            walFile.write(data);
            walFile.getFD().sync();


            dataFile.seek((long) (ref + 1) * PAGE_SIZE);
            dataFile.write(data);
            dataFile.getFD().sync();

            clearWAL();
            return ref;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void delete(int idx) {
        try {
            var listPage = new FreeList();
            listPage.nextRef = meta.freeListRef;
            var buffer = FreeList.encode(listPage).array();

            walFile.seek(walFile.length());
            walFile.writeInt(idx);
            walFile.writeInt(PAGE_SIZE);
            walFile.write(buffer);

            dataFile.seek((long)(idx + 1) * PAGE_SIZE);
            dataFile.write(buffer);
            dataFile.getFD().sync();

            clearWAL();
            meta.freeListRef = idx;
            writeMeta();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRoot(int idx) {
        meta = new Meta(idx, meta.freeListRef);

        writeMeta();
    }

    @Override
    public int getRoot() {
        if (meta != null) return meta.rootRef;

        readMeta();
        return meta.rootRef;
    }

    public void close() throws IOException {
        dataFile.close();
        walFile.close();
    }

    private void writeMeta() {
        assert meta != null;

        var buffer = Meta.encode(meta);

        try {
            walFile.seek(walFile.length());
            walFile.writeInt(0);
            walFile.writeInt(PAGE_SIZE);
            walFile.write(buffer.array());

            dataFile.seek(0L);
            dataFile.write(buffer.array());
            dataFile.getFD().sync();

            clearWAL();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeIfEmpty() {
        try {
            if (dataFile.length() < PAGE_SIZE) {
                meta = new Meta(UNDEFINED_REF, UNDEFINED_REF);
                writeMeta();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readMeta() {
        try {
            var bytes = new byte[PAGE_SIZE];
            dataFile.seek(0L);
            dataFile.readFully(bytes);
            meta = Meta.decode(ByteBuffer.wrap(bytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void recoverFromWAL() throws IOException {
        walFile.seek(0);
        while (walFile.getFilePointer() < walFile.length()) {
            int pageId = walFile.readInt();
            int length = walFile.readInt();
            byte[] data = new byte[length];
            walFile.readFully(data);
            dataFile.seek((long) (pageId + 1) * PAGE_SIZE);
            dataFile.write(data);
        }
        clearWAL();
    }

    private void clearWAL() throws IOException {
        walFile.setLength(0);
        walFile.getFD().sync();
    }
}

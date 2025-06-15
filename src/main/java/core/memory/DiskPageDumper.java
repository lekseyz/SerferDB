package core.memory;

import core.page.PageDumper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import static core.page.PagingConstants.PAGE_SIZE;
import static core.page.PagingConstants.UNDEFINED_REF;

public class DiskPageDumper implements PageDumper {
    private final FileChannel dataChannel;
    private final Path dbFile;
    private final Path tmpFile;
    private int nextPageId = 1;
    private Meta meta;

    public DiskPageDumper(Path dataPath) throws IOException {
        this.dbFile = dataPath;
        this.tmpFile = dataPath.resolveSibling(dataPath.getFileName() + ".tmp");
        if (!Files.exists(dbFile)) {
            Files.createFile(dbFile);
        }

        Files.copy(dbFile, tmpFile, StandardCopyOption.REPLACE_EXISTING);
        dataChannel = FileChannel.open(tmpFile,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE);
        initialize();
    }

    @Override
    public synchronized ByteBuffer get(int idx) {
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(PAGE_SIZE);
            dataChannel.read(buffer, (long) (idx + 1) * PAGE_SIZE);
            buffer.flip();
            return buffer;
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
                ByteBuffer buffer = ByteBuffer.allocateDirect(PAGE_SIZE);
                dataChannel.read(buffer, (long) (ref + 1) * PAGE_SIZE);
                buffer.flip();
                var listPage = FreeList.decode(buffer);
                meta.freeListRef = listPage.nextRef;
                writeMeta();
            } else {
                ref = nextPageId++;
            }

            bytes.rewind();
            dataChannel.write(bytes, (long) (ref + 1) * PAGE_SIZE);
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
            ByteBuffer encoded = FreeList.encode(listPage);

            encoded.rewind();
            dataChannel.write(encoded, (long) (idx + 1) * PAGE_SIZE);

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

    @Override
    public void close() {
        try {
            dataChannel.force(true); // atomic transactions - guarantee to write the updates on commit
            dataChannel.close();

            Files.move(tmpFile, dbFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeMeta() {
        assert meta != null;
        ByteBuffer buffer = Meta.encode(meta);

        try {
            buffer.rewind();
            dataChannel.write(buffer, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initialize() throws IOException {
        try {
            if (dataChannel.size() < PAGE_SIZE) {
                meta = new Meta(UNDEFINED_REF, UNDEFINED_REF);
                writeMeta();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readMeta() {
        try {
            ByteBuffer buf = ByteBuffer.allocate(PAGE_SIZE);
            dataChannel.read(buf, 0);
            buf.flip();
            meta = Meta.decode(buf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

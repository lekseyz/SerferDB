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
    public synchronized ByteBuffer get(int idx) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(PAGE_SIZE);
        dataChannel.read(buffer, (long) (idx + 1) * PAGE_SIZE);
        buffer.flip();
        return buffer;
    }

    @Override
    public synchronized int set(ByteBuffer bytes) throws IOException {
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
            ref = meta.nextNodeIdx++;
            writeMeta();
        }

        bytes.rewind();
        dataChannel.write(bytes, (long) (ref + 1) * PAGE_SIZE);
        return ref;
    }

    @Override
    public synchronized void delete(int idx) throws IOException {
        var listPage = new FreeList();
        listPage.nextRef = meta.freeListRef;
        ByteBuffer encoded = FreeList.encode(listPage);

        encoded.rewind();
        dataChannel.write(encoded, (long) (idx + 1) * PAGE_SIZE);

        meta.freeListRef = idx;
        writeMeta();
    }

    @Override
    public void setRoot(int idx) throws IOException {
        meta = new Meta(idx, meta.freeListRef, meta.nextNodeIdx);
        writeMeta();
    }

    @Override
    public int getRoot() {
        if (meta == null) throw new IllegalStateException("Meta cannot be null");
        return meta.rootRef;
    }

    @Override
    public void close() throws IOException {
        if (!dataChannel.isOpen())
            return;

        dataChannel.force(true); // atomic transactions - guarantee to write the updates on commit
        dataChannel.close();

        Files.move(tmpFile, dbFile, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void free() throws IOException {
        close();
        Files.delete(dbFile);
    }

    private void writeMeta() throws IOException{
        assert meta != null;
        ByteBuffer buffer = Meta.encode(meta);

        buffer.rewind();
        dataChannel.write(buffer, 0);
    }

    private void initialize() throws IOException {
        if (dataChannel.size() < PAGE_SIZE) {
            meta = new Meta(UNDEFINED_REF, UNDEFINED_REF);
            writeMeta();
        } else {
            readMeta();
        }
    }

    private void readMeta() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(PAGE_SIZE);
        dataChannel.read(buf, 0);
        buf.rewind();
        meta = Meta.decode(buf);
    }
}

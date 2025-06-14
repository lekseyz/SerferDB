package core.memory;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static core.page.PagingConstants.PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.*;

public class TestDumper {
    static Path dataPath;
    static Path walPath;

    @BeforeAll
    static void createPaths() throws IOException {
        dataPath = Paths.get("_test_data.dump");
        walPath = Paths.get("_test_wal.dump");

        for (var path : List.of(new Path[]{dataPath, walPath})) {
            if (Files.exists(path)){
                Files.delete(path);
            }
        }
    }

    @Test
    public void testWriteAndReadPage() throws IOException {
        DiskPageDumper dumper = new DiskPageDumper(dataPath, walPath);
        ByteBuffer buf = ByteBuffer.allocate(PAGE_SIZE);
        buf.putInt(42);
        buf.rewind();

        int pageId = dumper.set(buf);
        ByteBuffer readBuf = dumper.get(pageId);

        assertEquals(42, readBuf.getInt());
    }

    @Test
    public void testPageReuse() throws IOException {
        DiskPageDumper dumper = new DiskPageDumper(dataPath, walPath);

        ByteBuffer buf1 = ByteBuffer.allocate(PAGE_SIZE).putInt(123); buf1.rewind();
        int id1 = dumper.set(buf1);

        dumper.delete(id1);

        ByteBuffer buf2 = ByteBuffer.allocate(PAGE_SIZE).putInt(456); buf2.rewind();
        int id2 = dumper.set(buf2);

        assertEquals(id1, id2);
        ByteBuffer readBuf = dumper.get(id2);
        assertEquals(456, readBuf.getInt());
    }

}

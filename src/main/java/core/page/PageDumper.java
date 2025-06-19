package core.page;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface PageDumper {
    public ByteBuffer get(int idx) throws IOException;
    public int set(ByteBuffer bytes) throws IOException;
    public void delete(int idx) throws IOException;

    public void setRoot(int idx) throws IOException;
    public int getRoot() throws IOException;
    public void close() throws IOException;
    public void free() throws IOException;
}

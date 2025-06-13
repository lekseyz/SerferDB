package core.page;

import java.nio.ByteBuffer;

public interface PageDumper {
    public ByteBuffer get(int idx);
    public int set(ByteBuffer bytes);
    public boolean delete(int idx);
}

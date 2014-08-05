package org.opennms.newts.search.cassandra.lucene;


import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.lucene.store.IndexInput;

import com.google.common.base.Preconditions;


public class CassandraIndexInput extends IndexInput {

    private final CassandraFile m_file;
    private long m_pointer;
    private long m_offset = 0;

    CassandraIndexInput(CassandraFile file) {
        this(file, 0);
    }

    CassandraIndexInput(CassandraFile file, long offset) {
        super(file.getResourceDescription());
        m_file = Preconditions.checkNotNull(file, "file argument");
        m_offset = offset;
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public long getFilePointer() {
        return m_pointer;
    }

    @Override
    public void seek(long pos) throws IOException {
        m_pointer = pos;
    }

    @Override
    public long length() {
        return m_file.getLength();
    }

    @Override
    public IndexInput slice(String sliceDescription, long offset, final long length) throws IOException {
        return new CassandraIndexInput(m_file, m_pointer + offset) {
            @Override
            public long length() {
                return length;
            }
        };
    }

    @Override
    public byte readByte() throws IOException {
        byte b = getCurrentSegment().get();
        m_pointer += 1;
        return b;
    }

    @Override
    public void readBytes(byte[] b, int offset, int len) throws IOException {
        ByteBuffer buf = getCurrentSegment();

        if (len <= buf.remaining()) {
            buf.get(b, offset, len);
            m_pointer += len;
        }
        else {
            int remaining = buf.remaining();
            buf.get(b, offset, remaining);
            m_pointer += remaining;
            readBytes(b, offset + remaining, len - remaining);
        }

    }

    private ByteBuffer getCurrentSegment() throws IOException {
        ByteBuffer seg = m_file.getSegment(getCurrentSegmentNumber());
        seg.position(seg.position() + getCurrentSegmentOffset());
        return seg;
    }

    private int getCurrentSegmentOffset() {
        return (int)(getPosition() % CassandraFile.SEGMENT_SIZE);
    }
    
    private long getCurrentSegmentNumber() {
        return (getPosition() / CassandraFile.SEGMENT_SIZE) + 1;
    }

    private long getPosition() {
        return m_pointer + m_offset;
    }

}

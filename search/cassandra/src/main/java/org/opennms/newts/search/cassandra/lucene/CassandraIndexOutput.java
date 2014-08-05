package org.opennms.newts.search.cassandra.lucene;


import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.zip.CRC32;

import org.apache.lucene.store.IndexOutput;


public class CassandraIndexOutput extends IndexOutput {

    private final CassandraFile m_file;
    private final CRC32 m_crcHash = new CRC32();
    private long m_pointer = 0;

    public CassandraIndexOutput(CassandraFile file) {
        m_file = checkNotNull(file, "file argument");
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {
        m_file.close();
    }

    @Override
    public long getFilePointer() {
        return m_pointer;
    }

    @Override
    public long getChecksum() throws IOException {
        return m_crcHash.getValue();
    }

    @Override
    public void writeByte(byte b) throws IOException {
        m_crcHash.update(b);
        m_pointer += 1;
        m_file.writeBytes(new byte[] { b }, 0, 1);
    }

    @Override
    public void writeBytes(byte[] b, int offset, int length) throws IOException {
        m_crcHash.update(b, offset, length);
        m_pointer += length;
        m_file.writeBytes(b, offset, length);
    }

}

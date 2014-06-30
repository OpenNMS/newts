package org.opennms.newts.stress;


import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;


class InsertConfig extends Config {

    private int m_batchSize = 100;

    @Option(name = "-B", aliases = "--batch-size", metaVar = "<size>", usage = "Number of samples per batch.")
    void setBatchSize(int batchSize) throws CmdLineException {
        checkArgument(batchSize > 0, "Batch size must be greater than zero.");
        m_batchSize = batchSize;
    }

    int getBatchSize() {
        return m_batchSize;
    }

}

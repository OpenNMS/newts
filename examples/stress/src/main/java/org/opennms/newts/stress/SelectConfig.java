package org.opennms.newts.stress;


import org.kohsuke.args4j.Option;
import org.opennms.newts.api.Duration;


class SelectConfig extends Config {

    private Duration m_resolution = Duration.seconds(3600);
    private Duration m_selectLength = Duration.seconds(86400);

    // XXX: selectLength should be validated; selectLength should be greater than resolution
    @Option(name = "-sl", aliases = "--select-length", metaVar = "<length>", usage = "Length of select in seconds.")
    void setSelectLength(Duration selectLength) {
        m_selectLength = selectLength;
    }

    // XXX: resolution should be validated; resolution should be greater than interval
    @Option(name = "-R", aliases = "--resolution", metaVar = "<resolution>", usage = "Aggregate resolution in seconds.")
    void setResolution(Duration resolution) {
        m_resolution = resolution;
    }

    Duration getResolution() {
        return m_resolution;
    }

    Duration getSelectLength() {
        return m_selectLength;
    }

}

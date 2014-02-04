package org.opennms.newts.bench;


import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.MeasurementRepository;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.persistence.cassandra.CassandraMeasurementRepository;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;


public class Pull extends Base {

    @Option(name = "-h", usage = "Print this usage summary")
    private boolean m_needHelp = false;

    @Option(name = "-H", usage = "Cassandra hostname", metaVar = "HOST")
    private String m_hostname = "localhost";

    @Option(name = "-P", usage = "Cassandra port number", metaVar = "PORT")
    private int m_port = 9042;

    @Option(name = "-K", usage = "Cassandra keyspace", metaVar = "KEYSPACE")
    private String m_keyspace = "newts";

    @Option(name = "-r", usage = "Resource name", metaVar = "RESOURCE")
    private String m_resource = "localhost";

    @Option(name = "-s", usage = "Seconds per step", metaVar = "SECONDS")
    private int m_step = 300;

    @Option(name = "-l", usage = "Length of graph period", metaVar = "SECONDS")
    private int m_length = 86400;

    private MeasurementRepository m_repository;
    private Timer m_selectTimer;

    Pull(MetricRegistry registry) {
        super(registry);
        
        m_selectTimer = m_registry.timer(MetricRegistry.name(Pull.class, "select"));
    }

    String getHostname() {
        return m_hostname;
    }

    int getPort() {
        return m_port;
    }

    String getResource() {
        return m_resource;
    }

    int getStepSeconds() {
        return m_step;
    }

    int getLength() {
        return m_length;
    }

    String getKeyspace() {
        return m_keyspace;
    }

    void doMain(String[] args) {
        m_parser = new CmdLineParser(this);

        m_parser.setUsageWidth(80);

        try {
            m_parser.parseArgument(args);
        }
        catch (CmdLineException e) {
            printUsage(e.getMessage(), System.err);
            System.exit(1);
        }

        if (m_needHelp) {
            printUsage();
            System.exit(0);
        }

        m_repository = new CassandraMeasurementRepository(getKeyspace(), getHostname(), getPort(), m_registry);

        doPull();

        m_reporter.report();

        System.exit(0);
    }

    void doPull() {

        Timestamp start = Timestamp.fromEpochMillis(0);
        Timestamp end = start.plus(Duration.seconds(getLength()));
        long startTime = System.currentTimeMillis();

        Results r;
        Context ctx = m_selectTimer.time();
        
        try {
            r = m_repository.select(getResource(), start, end, Duration.seconds(getStepSeconds()));
        }
        finally {
            ctx.stop();
        }

        double elapsed = ((System.currentTimeMillis() - startTime) / 1000d);

        System.out.printf("Completed: %d results in %.3f seconds%n", r.getRows().size(), elapsed);

    }

    public static void main(String... args) {
        new Pull(new MetricRegistry()).doMain(args);
    }

}

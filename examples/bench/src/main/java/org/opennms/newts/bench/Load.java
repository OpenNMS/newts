package org.opennms.newts.bench;


import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.opennms.newts.api.Counter;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MeasurementRepository;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.persistence.cassandra.CassandraMeasurementRepository;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;


public class Load extends Base {

    private static final long UNSIGNED_32 = 0xFFFFFFFF;
    private static final long COUNTER_INC = 10000L;

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

    @Option(name = "-m", usage = "Metric name", metaVar = "METRIC")
    private String m_metricName = "metric0";

    @Option(name = "-i", usage = "Sample interval", metaVar = "INTERVAL")
    private long m_interval = 60;

    private MeasurementRepository m_repository;
    private long m_counter = 0;

    Load(MetricRegistry registry) {
        super(registry);
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

    String getMetricName() {
        return m_metricName;
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

        doLoad();

        m_reporter.report();

        System.exit(0);
    }

    // XXX: There is much hard-coding here...
    private void doLoad() {

        Timestamp current = Timestamp.fromEpochSeconds(0);
        Timestamp end = current.plus(Duration.seconds(86400 * 365)); // One year
        Duration interval = Duration.seconds(m_interval);
        List<Measurement> measurements = Lists.newArrayList();
        long startTime = System.currentTimeMillis();
        int numCompleted = 0;

        while (current.lt(end)) {
            measurements.add(getMeasurement(current));
            numCompleted += 1;

            if ((numCompleted % 500) == 0) {
                m_repository.insert(measurements);
                measurements = Lists.newArrayList();
                System.out.printf("Inserted %d measurements...%n", numCompleted);
            }

            current = current.plus(interval);
        }

        if ((measurements.size() > 0)) m_repository.insert(measurements);

        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        double rate = numCompleted / elapsedSeconds;

        System.out.printf("Complete: %d measurements in %d seconds (%.2f/sec)%n", numCompleted, elapsedSeconds, rate);
    }

    private Measurement getMeasurement(Timestamp timestamp) {
        return new Measurement(timestamp, getResource(), getMetricName(), MetricType.COUNTER, getCounterValue());
    }

    private synchronized Counter getCounterValue() {
        long newCount = m_counter + COUNTER_INC;
        long overage = UNSIGNED_32 - newCount;
        m_counter = (overage < 0) ? Math.abs(overage) : newCount;

        return new Counter(UnsignedLong.valueOf(m_counter));
    }

    @Override
    public String toString() {
        return String.format(
                "%s[hostname=%s, port=%s, resource=%s, metric=%s]",
                getClass().getSimpleName(),
                getHostname(),
                getPort(),
                getResource(),
                getMetricName());
    }

    public static void main(String... args) {
        new Load(new MetricRegistry()).doMain(args);
    }

}

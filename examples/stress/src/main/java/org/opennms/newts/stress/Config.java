package org.opennms.newts.stress;


import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Timestamp;


class Config {

    enum Command {
        INSERT, SELECT;
    }

    static {
        CmdLineParser.registerHandler(Timestamp.class, TimestampOptionHandler.class);
        CmdLineParser.registerHandler(Duration.class, DurationOptionHandler.class);
    }

    /** Number of seconds to keep Cassandra-stored samples. */
    static int CASSANDRA_TTL = 86400;

    private boolean m_needHelp = false;
    private int m_threads = 4;
    private String m_cassandraHost = "localhost";
    private int m_cassandraPort = 9042;
    private String m_cassandraKeyspace = "newts";
    private String m_cassandraCompression = "NONE";
    private Timestamp m_start = Timestamp.fromEpochSeconds(900000000);
    private Timestamp m_end = Timestamp.fromEpochSeconds(931536000);
    private Duration m_interval = Duration.seconds(300);
    private int m_numResources = 1;
    private int m_numMetrics = 1;
    private Command m_command;

    protected void checkArgument(boolean condition, String msg, Object... msgArgs) throws CmdLineException {
        if (!condition) {
            throw new CmdLineException(null, String.format(msg, msgArgs));

        }
    }

    @Argument(required = true, metaVar = "<command>", index = 0, usage = "The operation to run.")
    void setCommandArgument(String command) throws CmdLineException {
        try {
            m_command = Command.valueOf(command.toUpperCase());
        }
        catch (IllegalArgumentException ex) {
            throw new CmdLineException(null, String.format("Unknown command: %s", command));
        }
    }

    @Option(name="-h", aliases="--help", usage="Print usage informations.")
    void setHelp(boolean help) {
        m_needHelp = help;
    }

    @Option(name = "-n", aliases = "--num-threads", metaVar = "<threads>", usage = "Concurrency level.")
    void setThreads(int threads) throws CmdLineException {
        checkArgument(threads > 0, "-n/--num-threads must be at least 1");
        m_threads = threads;
    }

    @Option(name = "-H", aliases = "--cassandra-host", metaVar = "<hostname>", usage = "Cassandra hostname.")
    void setCassandraHost(String host) {
        m_cassandraHost = host;
    }

    @Option(name = "-p", aliases = "--cassandra-port", metaVar = "<port>", usage = "Cassandra port number.")
    void setCassandraPort(int port) throws CmdLineException {
        checkArgument(port > 0, "Cassandra port number must be greater than zero");
        m_cassandraPort = port;
    }

    @Option(name = "-k", aliases = "--cassandra-keyspace", metaVar = "<keyspace>", usage = "Cassandra keyspace.")
    void setCassandraKeyspace(String keyspace) {
        m_cassandraKeyspace = keyspace;
    }

    @Option(name = "-s", aliases = "--start", metaVar = "<start>", usage = "ISO8601 formatted start time.")
    void setStart(Timestamp start) {
        m_start = start;
    }

    @Option(name = "-e", aliases = "--end", metaVar = "<end>", usage = "ISO8601 formatted ending time.")
    void setEnd(Timestamp end) {
        m_end = end;
    }

    @Option(name = "-i", aliases = "--interval", metaVar = "<interval>", usage = "Sample interval in seconds.")
    void setInterval(Duration interval) {
        m_interval = interval;
    }

    @Option(name = "-r", aliases = "--num-resources", metaVar = "<resources>", usage = "Number of resources.")
    void setNumResources(int numResources) throws CmdLineException {
        checkArgument(numResources > 0, "Number of resources must be greater than zero.");
        m_numResources = numResources;
    }

    @Option(name = "-m", aliases = "--num-metrics", metaVar = "<metrics>", usage = "Number of metrics.")
    void setNumMetrics(int numMetrics) throws CmdLineException {
        checkArgument(numMetrics > 0, "Number of metrics must be greater than zero.");
        m_numMetrics = numMetrics;
    }

    String getCassandraHost() {
        return m_cassandraHost;
    }

    int getCassandraPort() {
        return m_cassandraPort;
    }

    String getCassandraKeyspace() {
        return m_cassandraKeyspace;
    }

    String getCassandraCompression() {
        return m_cassandraCompression;
    }

    Command getCommand() {
        return m_command;
    }

    boolean needHelp() {
        return m_needHelp;
    }

    Timestamp getStart() {
        return m_start;
    }

    Timestamp getEnd() {
        return m_end;
    }

    Duration getInterval() {
        return m_interval;
    }

    int getNumResources() {
        return m_numResources;
    }

    int getNumMetrics() {
        return m_numMetrics;
    }

    int getThreads() {
        return m_threads;
    }

    // There is (presently )no command option to assign this; Hard-coded to 2x the sample interval.
    Duration getHeartbeat() {
        return m_interval.times(2);
    }

    String[] getResources() {
        String[] resources = new String[getNumResources()];
        for (int i = 0; i < getNumResources(); i++) {
            resources[i] = String.format("r%d", i);
        }
        return resources;
    }

    String[] getMetrics() {
        String[] metrics = new String[getNumMetrics()];
        for (int i = 0; i < getNumMetrics(); i++) {
            metrics[i] = String.format("m%d", i);
        }
        return metrics;
    }

}

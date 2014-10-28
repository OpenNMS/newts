package org.opennms.newts.cli;


import org.kohsuke.args4j.Option;


class Config {

    @Option(name = "-k", aliases = "--cassandra-keyspace", usage = "Cassandra keyspace name (default: newts)", metaVar = "KEYSPACE")
    private String m_cassandraKeyspace = "newts";

    @Option(name = "-H", aliases = "--cassandra-hostname", usage = "Cassandra hostname (default: localhost)", metaVar = "HOSTNAME")
    private String m_cassandraHostname = "localhost";

    @Option(name = "-p", aliases = "--cassandra-port", usage = "Cassandra port number (default: 9042)", metaVar = "PORT")
    private int m_cassandraPort = 9042;

    @Option(name = "-I", aliases = "--enable-search", usage = "Enable search operations (default: false")
    private boolean m_searchEnabled = false;

    @Option(name = "-Ts", aliases = "--cassandra-samples-ttl", usage = "Cassandra samples TTL (default: 1 year)", metaVar = "TTL")
    private int m_cassandraSamplesTTL = 31536000;

    @Option(name = "-Tr", aliases = "--cassandra-search-ttl", usage = "Cassandra TTL for search (default: 1 year)", metaVar = "TTL")
    private int m_cassandraSearchTTL = 31536000;
    
    @Option(name = "-h", aliases = "--help", usage = "Output this usage summary")
    private boolean m_doHelp = false;

    String getCassandraKeyspace() {
        return m_cassandraKeyspace;
    }

    String getCassandraHostname() {
        return m_cassandraHostname;
    }

    int getCassandraPort() {
        return m_cassandraPort;
    }

    boolean isSearchEnabled() {
        return m_searchEnabled;
    }

    int getCassandraSamplesTTL() {
        return m_cassandraSamplesTTL;
    }

    int getCassandraSearchTTL() {
        return m_cassandraSearchTTL;
    }

    boolean doHelp() {
        return m_doHelp;
    }

}

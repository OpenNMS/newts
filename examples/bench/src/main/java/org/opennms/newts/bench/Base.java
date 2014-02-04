package org.opennms.newts.bench;


import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.PrintStream;

import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;


public class Base {

    protected static final Logger LOG = LoggerFactory.getLogger(Base.class);

    protected MetricRegistry m_registry;
    protected Slf4jReporter m_reporter;
    protected CmdLineParser m_parser;

    protected Base(MetricRegistry registry) {
        m_registry = registry;
        
        m_reporter = Slf4jReporter.forRegistry(m_registry)
                .outputTo(LOG)
                .convertRatesTo(SECONDS)
                .convertDurationsTo(MILLISECONDS)
                .build();
        
        m_reporter.start(1, SECONDS);
    }

    protected void printUsage() {
        printUsage(null, System.out);
    }

    protected void printUsage(String msg, PrintStream stream) {
        if (msg != null) stream.println(msg);
        stream.println("java Load [options...]");
        m_parser.printUsage(stream);
    }

}

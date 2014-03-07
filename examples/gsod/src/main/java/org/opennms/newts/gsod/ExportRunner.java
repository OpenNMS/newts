package org.opennms.newts.gsod;


import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;

import com.google.common.base.Optional;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class ExportRunner {

    private final SampleRepository m_repository;
    private final CmdLineParser m_parser = new CmdLineParser(this);

    @Option(name = "-r", usage = "resource name to query (required)")
    private String m_resource;

    @Option(name = "-m", usage = "metric to query (required)")
    private String m_metric;

    @Option(name = "-s", usage = "query start time in milliseconds")
    private Long m_start;

    @Option(name = "-e", usage = "query end time milliseconds")
    private Long m_end;

    @Option(name = "-h", usage = "print this usage information")
    private boolean m_needsHelp;

    @Inject
    public ExportRunner(SampleRepository repository) {
        m_repository = repository;
    }

    private void printUsage(PrintStream writer) {
        writer.println("Usage: java ExportRunner [options...] ");
        m_parser.printUsage(writer);
    }

    private int go(String[] args) {

        m_parser.setUsageWidth(80);

        try {
            m_parser.parseArgument(args);
        }
        catch (CmdLineException e) {
            System.err.println(e.getMessage());
            printUsage(System.err);
            return 1;
        }

        if (m_needsHelp) {
            printUsage(System.out);
            return 0;
        }

        if (m_resource == null || m_metric == null) {
            System.err.println("Missing required argument(s)");
            printUsage(System.err);
            return 1;
        }

        System.out.printf("timestamp,%s%n", m_metric);

        for (Results.Row<Sample> row : m_repository.select(m_resource, timestamp(m_start), timestamp(m_end))) {
            System.out.printf("%s,%.2f%n", row.getTimestamp().asDate(), row.getElement(m_metric).getValue().doubleValue());
        }

        return 0;
    }

    private static Optional<Timestamp> timestamp(Long arg) {
        if (arg == null) return Optional.<Timestamp> absent();
        return Optional.of(new Timestamp(arg, TimeUnit.MILLISECONDS));
    }

    public static void main(String... args) {

        Injector injector = Guice.createInjector(new Config());
        ExportRunner runner = injector.getInstance(ExportRunner.class);

        System.exit(runner.go(args));

    }

}

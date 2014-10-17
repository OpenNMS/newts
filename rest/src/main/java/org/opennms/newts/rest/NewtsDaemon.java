package org.opennms.newts.rest;


import java.io.File;
import java.io.PrintStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.skife.gressil.Daemon;


public class NewtsDaemon {

    private static class CommandLine {
        @Option(name = "-p", aliases = {"--pid"}, metaVar = "PIDFILE", usage = "Path to PID file (default: newtsd.pid)")
        private String m_pidFilename = "newtsd.pid";

        @Option(name = "-D", aliases = {"--daemonize"}, usage = "Detach and run in the background")
        private boolean m_isDaemon;

        @Option(name = "-c", aliases = {"--config"}, metaVar = "CFGFILE", usage = "Path to configuration file (required)", required = true)
        private String m_configFilename;

        @Option(name = "-h", aliases = {"--help"}, usage = "Print usage informations")
        private boolean m_needHelp;

        private String getPidFilename() {
            return m_pidFilename;
        }

        private boolean isDaemon() {
            return m_isDaemon;
        }

        private String getConfigFilename() {
            return m_configFilename;
        }

        private boolean needHelp() {
            return m_needHelp;
        }

    }

    private static void usage(CmdLineParser parser, PrintStream stream) {
        stream.println("Usage: java NewtsDaemon -c CFGFILE [-D] [-p PIDFILE] [-h]");
        parser.printUsage(stream);
    }

    public static void main(String[] args) throws Exception {

        CommandLine cmdLine = new CommandLine();
        CmdLineParser parser = new CmdLineParser(cmdLine);

        try {
            parser.parseArgument(args);
        }
        catch (CmdLineException e) {
            System.err.println(e.getLocalizedMessage());
            usage(parser, System.err);
            System.exit(1);
        }

        // Help requested
        if (cmdLine.needHelp()) {
            usage(parser, System.out);
            System.exit(0);
        }

        // Configuration file does not exist.
        if (!new File(cmdLine.getConfigFilename()).isFile()) {
            System.err.printf("No such file: %s%n", cmdLine.getConfigFilename());
            System.exit(1);
        }

        File pidFile = new File(cmdLine.getPidFilename());

        // Daemonize?
        if (cmdLine.isDaemon()) {
            new Daemon().withMainArgs(args).withPidFile(pidFile).daemonize();
        }

        new NewtsService().run(new String[] { "server", cmdLine.getConfigFilename() });

    }

}

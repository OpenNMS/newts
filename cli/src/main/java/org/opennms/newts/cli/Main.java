package org.opennms.newts.cli;


import java.io.IOException;
import java.io.PrintStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;


public class Main {

    public static void main(String[] args) throws IOException {

        Config config = new Config();
        CmdLineParser parser = new CmdLineParser(config);

        try {
            parser.parseArgument(args);
        }
        catch (CmdLineException e) {
            System.err.println(e.getLocalizedMessage());
            printUsage(System.err, parser);
            System.exit(1);
        }

        if (config.doHelp()) {
            printUsage(System.out, parser);
            System.exit(0);
        }

        try {
            new CommandLoop(config).go();
        }
        finally {
            CommandLoop.restore();
        }

        System.exit(0);
    }

    private static void printUsage(PrintStream stream, CmdLineParser parser) {
        stream.println("java o.o.newts.cli.Main [options]");
        parser.printUsage(stream);
    }

}

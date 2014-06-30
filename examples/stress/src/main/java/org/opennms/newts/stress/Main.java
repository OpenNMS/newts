package org.opennms.newts.stress;


import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;


public class Main {

    private static Config getConfig(String... args) {

        Config config = new Config();
        CmdLineParser parser = new CmdLineParser(config);

        try {
            parser.parseArgument(args);
        }
        catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            System.err.printf("%njava %s [options] <command>%n%n", Main.class.getCanonicalName());
            parser.printUsage(System.err);
            System.exit(1);
        }

        return config;
    }

    public static void main(String... args) throws InterruptedException {

        Config config = getConfig(args);
        Dispatcher dispatcher;

        switch (config.getCommand()) {
            case INSERT:
                dispatcher = new InsertDispatcher(config);
                break;
            case SELECT:
                dispatcher = new SelectDispatcher(config);
                break;
            default:
                throw new RuntimeException("Unknown command enum; Report as bug!!");
        }

        dispatcher.go();
        dispatcher.printReport();

        System.exit(0);

    }

}

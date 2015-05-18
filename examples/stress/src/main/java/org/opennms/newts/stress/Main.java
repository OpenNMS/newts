/*
 * Copyright 2014, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.stress;


import java.io.PrintStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.google.common.base.Joiner;


public class Main {

    private static void printSynopsis(PrintStream stream) {
        stream.printf("%njava %s <command> [options...]%n%n", Main.class.getCanonicalName());
    }

    private static void printUsage(PrintStream stream, CmdLineParser parser) {
        printSynopsis(stream);
        parser.printUsage(stream);
    }

    private static void printCmdUsage(PrintStream stream) {
        printSynopsis(stream);
        stream.printf("Available commands: %s%n%n", Joiner.on(", ").join(Config.Command.values()));
    }

    private static void parseArguments(Config config, String... args) {
        CmdLineParser parser = new CmdLineParser(config);

        try {
            parser.parseArgument(args);
        }
        catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            printUsage(System.err, parser);
            System.exit(1);
        }

        if (config.needHelp()) {
            printUsage(System.out, parser);
            System.exit(0);
        }
    }

    public static void main(String... args) throws InterruptedException {

        // Manually grok the command argument in order to conditionally apply different options.
        if (args.length < 1) {
            System.err.println("Missing command argument.");
            printCmdUsage(System.err);
            System.exit(1);
        }

        Config.Command command = null;

        try {
            command = Config.Command.valueOf(args[0].toUpperCase());
        }
        catch (IllegalArgumentException ex) {
            System.err.println("Unknown command: " + args[0]);
            printCmdUsage(System.err);
            System.exit(1);
        }

        Config config;
        Dispatcher dispatcher;

        switch (command) {
            case INSERT:
                config = new InsertConfig();
                parseArguments(config, args);
                dispatcher = new InsertDispatcher((InsertConfig) config);
                break;
            case SELECT:
                config = new SelectConfig();
                parseArguments(config, args);
                dispatcher = new SelectDispatcher((SelectConfig) config);
                break;
            default:
                throw new RuntimeException("Unknown command enum; Report as bug!!");
        }

        dispatcher.go();
        dispatcher.printReport();

        System.exit(0);

    }

}

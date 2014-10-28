package org.opennms.newts.cli;


import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.StringsCompleter;
import jline.console.history.FileHistory;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.cli.parse.NewtsCliLexer;
import org.opennms.newts.cli.parse.NewtsCliParser;
import org.opennms.newts.cli.parse.ReportableError;
import org.opennms.newts.cli.parse.SamplesGet;
import org.opennms.newts.cli.parse.Statement;

import com.google.inject.Guice;
import com.google.inject.Injector;


class CommandLoop {

    private static final DateFormat ISO_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    private final ConsoleReader m_console;
    private final SampleRepository m_repository;

    CommandLoop(Config config) throws IOException {
        checkNotNull(config, "config argument");

        m_console = new ConsoleReader();

        File home = new File(System.getProperty("user.home"));
        FileHistory history = new FileHistory(new File(home, ".newts_history"));
        m_console.setHistory(history);

        m_console.setPrompt("newts> ");
        
        m_console.addCompleter(
                new ArgumentCompleter(
                        new StringsCompleter("get"),
                        new StringsCompleter("samples"),
                        new StringsCompleter("where")));
        m_console.addCompleter(
                new ArgumentCompleter(
                        new StringsCompleter("exit", "quit")));
        m_console.addCompleter(
                new ArgumentCompleter(
                        new StringsCompleter("help"),
                        new StringsCompleter("get", "exit")));

        Injector injector = Guice.createInjector(new StandardGuiceModule(), new CassandraGuiceModule(config));
        m_repository = injector.getInstance(SampleRepository.class);

    }

    private void println(String value, Object... args) throws IOException {
        m_console.println(String.format(value, args));
    }

    private String repeat(Character c, int times) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < times; i++) s.append(c);
        return s.toString();
    }

    private void doSamplesGet(SamplesGet get) throws IOException {
        int count = 0;

        // Header
        println(" %-24s | %-25s | %-8s | %-11s", "Timestamp", "Name", "Type", "Value");
        println("-%s-+-%s-+-%s-+-%s-", repeat('-', 24), repeat('-', 25), repeat('-', 8), repeat('-', 11));

        // Table
        for (Row<Sample> r : m_repository.select(get.getResource(), get.getRangeStart(), get.getRangeEnd())) {
            for (Sample s : r.getElements()) {
                println(
                        " %24s | %25s | %8s | %11s",
                        ISO_FORMATTER.format(s.getTimestamp().asDate()),
                        s.getName(),
                        s.getType(),
                        s.getValue());
                count++;
            }
        }

        println("%n%d samples", count);
    }

    void go() {

        String line = null;

        try {
            outer: while ((line = m_console.readLine()) != null) {
                try {
                    // Ignore empty strings
                    if (line.trim().length() == 0) {
                        continue;
                    }

                    Statement statement = parseStatement(line);

                    switch (statement.getType()) {
                        case EXIT:
                            break outer;

                        case SAMPLES_GET:
                            doSamplesGet((SamplesGet) statement);
                            break;

                        default:
                            throw new RuntimeException(
                                    String.format(
                                            "Unknown statement type (%s); Report this as BUG!",
                                            statement.getType()));
                    }

                }
                catch (ReportableError | RecognitionException e) {
                    println(e.getLocalizedMessage());
                }
            }
        
            ((FileHistory)m_console.getHistory()).flush();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println();

    }

    static void restore() {
        try {
            TerminalFactory.get().restore();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Statement parseStatement(String line) throws RecognitionException {
        CharStream stream = new ANTLRStringStream(line);
        NewtsCliLexer lexer = new NewtsCliLexer(stream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        NewtsCliParser parser = new NewtsCliParser(tokenStream);

        return parser.r();
    }

}

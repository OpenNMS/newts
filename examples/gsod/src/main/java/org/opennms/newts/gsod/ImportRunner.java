package org.opennms.newts.gsod;


import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class ImportRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ImportRunner.class);

    public static void main(String... args) throws IOException {

        File root;

        if (args.length > 0) {
            root = new File(args[0]);
            if (!root.isDirectory()) {
                System.err.printf("No such directory: %s%n", args[0]);
                System.exit(1);
            }
        }
        else {
            root = new File(System.getProperty("user.dir"));
        }

        LOG.debug("Scanning {} for GSOD data files...", root);

        Slf4jReporter reporter = null;

        try {

            // Setup the injector
            Injector injector = Guice.createInjector(new Config(), new AbstractModule() {

                @Override
                protected void configure() {

                    BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(100);
                    RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
                    ThreadPoolExecutor executor = new ThreadPoolExecutor(20, 20, 30L, SECONDS, queue, handler);

                    bind(ThreadPoolExecutor.class).toInstance(executor);

                }
            });

            // Setup the slf4j metrics reporter
            MetricRegistry metrics = injector.getInstance(MetricRegistry.class);
            reporter = Slf4jReporter.forRegistry(metrics)
                                    .outputTo(LOG)
                                    .convertRatesTo(SECONDS)
                                    .convertDurationsTo(MILLISECONDS)
                                    .build();
            reporter.start(1, SECONDS);

            // Add a gauge to measure elapsed time in seconds
            final long start = System.currentTimeMillis();
            metrics.register("elapsed-seconds", new Gauge<Double>() {

                @Override
                public Double getValue() {
                    return ((double)(System.currentTimeMillis() - start) / 1000);
                }
            });
            
            // Kick-off the import
            Files.walkFileTree(root.toPath(), injector.getInstance(FileVisitor.class));

            // Shutdown the executor, and wait for queued jobs to finish
            ThreadPoolExecutor executor = injector.getInstance(ThreadPoolExecutor.class);
            executor.shutdown();
            executor.awaitTermination(15, SECONDS);

        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        reporter.report();

        System.exit(0);

    }

}

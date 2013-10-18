package org.opennms.newts.gsod;


import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.opennms.newts.api.MeasurementRepository;
import org.opennms.newts.cassandra.CassandraMeasurementRepository;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;


public class Config extends AbstractModule {

    @Override
    protected void configure() {

        bind(MeasurementRepository.class).to(CassandraMeasurementRepository.class);

        bind(String.class).annotatedWith(Names.named("cassandraKeyspace")).toInstance("newts");
        bind(String.class).annotatedWith(Names.named("cassandraHost")).toInstance("localhost");
        bind(Integer.class).annotatedWith(Names.named("cassandraPort")).toInstance(9042);

        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(100);
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(20, 20, 30L, SECONDS, queue, handler);

        bind(ThreadPoolExecutor.class).toInstance(executor);

        bind(MetricRegistry.class).toInstance(new MetricRegistry());

    }

}

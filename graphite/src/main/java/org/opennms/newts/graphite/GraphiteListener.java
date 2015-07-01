package org.opennms.newts.graphite;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;

public class GraphiteListener implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(GraphiteListener.class);

    private final GraphiteInitializer m_initializer;
    private final int m_listen;

    private EventLoopGroup m_bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup m_workerGroup = new NioEventLoopGroup();

    @Inject
    public GraphiteListener(GraphiteInitializer initializer, @Named("graphite.port") int port) {
        m_initializer = initializer;
        m_listen = port;
    }

    public void run() {
        try {
            ServerBootstrap bStrap = new ServerBootstrap();
            bStrap.group(m_bossGroup, m_workerGroup);
            bStrap.channel(NioServerSocketChannel.class);
            bStrap.handler(new LoggingHandler(LogLevel.INFO));
            bStrap.childHandler(m_initializer);
            Channel ch = bStrap.bind(this.m_listen).sync().channel();

            ch.closeFuture().sync();
        }
        catch (InterruptedException e) {
            LOG.info("Interrupted; Shutting down!");
        }
        finally {
            m_bossGroup.shutdownGracefully();
            m_workerGroup.shutdownGracefully();
        }
    }

    public static void main(String... args) throws InterruptedException {
        new GraphiteListener(new GraphiteInitializer(new SampleRepository() {

            @Override
            public Results<Measurement> select(Context context, Resource arg0, Optional<Timestamp> arg1, Optional<Timestamp> arg2,
                    ResultDescriptor arg3, Optional<Duration> arg4) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Results<Sample> select(Context context, Resource arg0, Optional<Timestamp> arg1, Optional<Timestamp> arg2) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void insert(Collection<Sample> arg0, boolean arg1) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void insert(Collection<Sample> arg0) {
                System.out.printf("MOCK INSERT! (%d samples)%n", arg0.size());
            }
        }, new MetricRegistry()), 2003).run();
    }

}

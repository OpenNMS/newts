package org.opennms.newts.graphite;

import static com.codahale.metrics.MetricRegistry.name;

import javax.inject.Inject;

import org.opennms.newts.api.SampleRepository;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class GraphiteInitializer extends ChannelInitializer<SocketChannel> {
    private static final StringDecoder DECODER = new StringDecoder();
    private static final StringEncoder ENCODER = new StringEncoder();

    private final SampleRepository m_repository;
    private final Counter m_protocolErrors;
    private final Counter m_storageErrors;

    @Inject
    public GraphiteInitializer(SampleRepository repostory, MetricRegistry registry) {
        m_repository = repostory;
        m_protocolErrors = registry.counter(name("graphite-listener", "protocol-errors"));
        m_storageErrors = registry.counter(name("graphite-listener", "storage-errors"));
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // Add the text line codec combination first,
        pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        // the encoder and decoder are static as these are sharable
        pipeline.addLast(DECODER);
        pipeline.addLast(ENCODER);
        // and then business logic.
        pipeline.addLast(new GraphiteHandler(m_repository, this));
    }

    void protocolErrorsInc() {
        m_protocolErrors.inc();
    }

    void storageErrorsInc() {
        m_storageErrors.inc();
    }
}

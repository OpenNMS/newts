package org.opennms.newts;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.persistence.cassandra.CassandraSampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;


/**
 * The Newts producer.
 */
public class NewtsProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(NewtsProducer.class);
    private final SampleRepository m_repository;

    public NewtsProducer(NewtsEndpoint endpoint) {
        super(endpoint);

        LOG.debug("Creating Newts producer");

        m_repository = new CassandraSampleRepository(
                endpoint.getKeyspace(),
                endpoint.getHostname(),
                endpoint.getPort(),
                new MetricRegistry());

    }

    public void process(Exchange exchange) throws Exception {
        checkArgument(exchange.getIn().getBody() instanceof Collection);

        Collection<Sample> samples = getTypeSafeCollection(
                (Collection<?>) exchange.getIn().getBody(),
                Sample.class);

        m_repository.insert(samples);

    }

    @SuppressWarnings("unchecked")
    private static <T> Collection<T> getTypeSafeCollection(Collection<?> collection, Class<T> cls) {
        checkNotNull(collection, "collection argument");
        checkNotNull(cls, "cls argument");

        for (Object obj : collection) {
            if (!cls.isAssignableFrom(obj.getClass())) {
                throw new ClassCastException(String.format(
                        "Cannot cast element of Collection from %s to %s",
                        obj.getClass().getName(),
                        cls.getName()));
            }
        }

        return (Collection<T>) collection;
    }

}

package org.opennms.newts;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MeasurementRepository;
import org.opennms.newts.persistence.cassandra.CassandraMeasurementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Newts producer.
 */
public class NewtsProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(NewtsProducer.class);
    private final MeasurementRepository m_repository;

    public NewtsProducer(NewtsEndpoint endpoint) {
        super(endpoint);

        LOG.debug("Creating Newts producer");

        m_repository = new CassandraMeasurementRepository(endpoint.getKeyspace(), endpoint.getHostname(), endpoint.getPort());

    }

    public void process(Exchange exchange) throws Exception {
        checkArgument(exchange.getIn().getBody() instanceof Collection);

        Collection<Measurement> measurements = getTypeSafeCollection(
                (Collection<?>) exchange.getIn().getBody(),
                Measurement.class);

        m_repository.insert(measurements);

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

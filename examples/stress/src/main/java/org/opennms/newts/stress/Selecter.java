package org.opennms.newts.stress;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.BlockingQueue;

import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.query.ResultDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Stress worker for selecting {@link Measurement measurements} with the Java native API.
 * 
 * @author eevans
 */
public class Selecter extends Worker {

    private static final Logger LOG = LoggerFactory.getLogger(Selecter.class);
    private final SampleRepository m_repository;
    private final ResultDescriptor m_rDescriptor;
    private final BlockingQueue<Query> m_queue;

    public Selecter(int sequence, SampleRepository repository, ResultDescriptor rDescriptor, BlockingQueue<Query> queue) {
        super(String.format("SELECTER-%d", sequence));

        m_repository = checkNotNull(repository, "repository argument");
        m_rDescriptor = checkNotNull(rDescriptor, "rDescriptor argument");
        m_queue = checkNotNull(queue, "queue argument");

        setDaemon(true);
        start();

    }

    @Override
    public void run() {
        Query query;

        try {
            while (true) {
                if ((query = m_queue.poll()) == null) {
                    if (isShutdown()) {
                        break;
                    }
                    else {
                        Thread.sleep(250);
                        continue;
                    }
                }

                LOG.debug(
                        "Selecting from {} to {} for resource {} at resolution {}",
                        query.getStart(),
                        query.getEnd(),
                        query.getResource(),
                        query.getResolution());
                Results<Measurement> results = m_repository.select(
                        query.getResource(),
                        query.getStart(),
                        query.getEnd(),
                        m_rDescriptor,
                        query.getResolution());
                LOG.debug("Select returned {} rows.", results.getRows().size());

            }
        }
        catch (InterruptedException e) {
            LOG.warn("Interrupted!");
        }
    }

}

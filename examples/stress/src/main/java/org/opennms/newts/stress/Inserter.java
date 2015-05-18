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


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;

import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Stress worker for inserting {@link Sample samples} with the Java native API.
 * 
 * @author eevans
 */
public class Inserter extends Worker {

    private static final Logger LOG = LoggerFactory.getLogger(Inserter.class);
    private final SampleRepository m_repository;
    private final BlockingQueue<Collection<Sample>> m_queue;

    public Inserter(int sequence, SampleRepository repository, BlockingQueue<Collection<Sample>> queue) {
        super(String.format("INSERTER-%d", sequence));

        m_repository = checkNotNull(repository, "repository argument");
        m_queue = checkNotNull(queue, "queue argument");

        setDaemon(true);
        start();

    }

    @Override
    public void run() {
        Collection<Sample> samples;

        try {
            while (true) {
                if ((samples = m_queue.poll()) == null) {
                    if (isShutdown()) {
                        break;
                    }
                    else {
                        Thread.sleep(250);
                        continue;
                    }
                }

                LOG.debug("Inserting {} samples", samples.size());
                m_repository.insert(samples);

            }
        }
        catch (InterruptedException e) {
            LOG.warn("Interrupted!");
        }

    }

}

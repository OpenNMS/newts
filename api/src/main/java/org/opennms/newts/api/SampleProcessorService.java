/*
 * Copyright 2016, The OpenNMS Group
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
package org.opennms.newts.api;


import java.util.Collection;
import java.util.concurrent.TimeUnit;

public interface SampleProcessorService {

    void submit(final Collection<Sample> samples);

    /** Calls <code>shutdown()</code> on the underlying thread pool executor if any. */
    void shutdown() throws InterruptedException;

    /**
     * Invokes <code>awaitShutdown()</code> on the underlying thread pool executor if any.
     *
     * @param timeout
     *            the maximum time to wait
     * @param unit
     *            the time unit of the timeout argument
     * @return {@code true} if this executor terminated and {@code false} if the timeout elapsed
     *         before termination
     * @throws InterruptedException
     */
    public boolean awaitShutdown(long timeout, TimeUnit unit) throws InterruptedException;

}

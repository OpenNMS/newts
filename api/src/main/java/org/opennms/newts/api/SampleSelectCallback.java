/*
 * Copyright 2015, The OpenNMS Group
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

/**
 * Allows callers to hook into the sample selection and processing calls
 * without exposing implementation details.
 *
 * This was originally written to give the caller to limit the number of
 * concurrent calls to ResultProcessor#process, while allowing for a
 * greater number of threads to retrieve the samples.
 *
 * @author jwhite
 */
public interface SampleSelectCallback {

    /**
     * Called after the raw samples have been fetched from the underlying storage,
     * but before these are processed or aggregated.
     */
    void beforeProcess();

    /**
     * Called after the raw samples have been processed, regardless if
     * there were any errors or not.
     *
     * Should not be called if {@link #beforeProcess()} threw a runtime exception.
     */
    void afterProcess();
}

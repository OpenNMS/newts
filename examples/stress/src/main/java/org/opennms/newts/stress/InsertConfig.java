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


import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;


class InsertConfig extends Config {

    private int m_batchSize = 100;

    @Option(name = "-B", aliases = "--batch-size", metaVar = "<size>", usage = "Number of samples per batch.")
    void setBatchSize(int batchSize) throws CmdLineException {
        checkArgument(batchSize > 0, "Batch size must be greater than zero.");
        m_batchSize = batchSize;
    }

    int getBatchSize() {
        return m_batchSize;
    }

}

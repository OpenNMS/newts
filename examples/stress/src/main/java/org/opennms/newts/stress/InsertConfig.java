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

    private boolean m_enableSearch = false;

    private boolean m_enableHierarchicalIndexing = false;

    @Option(name = "-B", aliases = "--batch-size", metaVar = "<size>", usage = "Number of samples per batch.")
    void setBatchSize(int batchSize) throws CmdLineException {
        checkArgument(batchSize > 0, "Batch size must be greater than zero.");
        m_batchSize = batchSize;
    }

    int getBatchSize() {
        return m_batchSize;
    }

    @Option(name = "-S", aliases = "--enable-search", usage = "Enable search indexing.")
    void setEnableSearch(boolean enableSearch) throws CmdLineException {
        m_enableSearch = enableSearch;
    }

    boolean isSearchEnabled() {
        return m_enableSearch;
    }

    @Option(name = "-Z", aliases = "--enable-hierarchical-indexing", usage = "Enable hierarchical indeing.")
    void setEnableHierarchicalIndexing(boolean enableHierarchicalIndexing) throws CmdLineException {
        m_enableHierarchicalIndexing = enableHierarchicalIndexing;
    }

    public boolean isHierarchicalIndexingEnabled() {
        return m_enableHierarchicalIndexing;
    }
}

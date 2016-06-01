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
package org.opennms.newts.cassandra.search;

import javax.inject.Inject;
import javax.inject.Named;

public class CassandraIndexingOptions {

    private final boolean m_enableHierarchicalIndexing;
    private final boolean m_indexUsingDefaultTerm;
    private final boolean m_indexResourceTerms;

    private final int m_maxBatchSize;

    public static class Builder {
        private int maxBatchSize = 16;
        private boolean enableHierarchicalIndexing = true;
        private boolean indexUsingDefaultTerm = true;
        private boolean indexResourceTerms = true;

        public Builder withHierarchicalIndexing(boolean enableHierarchicalIndexing) {
            this.enableHierarchicalIndexing = enableHierarchicalIndexing;
            return this;
        }

        public Builder withIndexUsingDefaultTerm(boolean indexUsingDefaultTerm) {
            this.indexUsingDefaultTerm = indexUsingDefaultTerm;
            return this;
        }

        public Builder withIndexResourceTerms(boolean indexResourceTerms) {
            this.indexResourceTerms = indexResourceTerms;
            return this;
        }

        public Builder withMaxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
            return this;
        }

        public CassandraIndexingOptions build() {
            return new CassandraIndexingOptions(this);
        }
    }

    public CassandraIndexingOptions(CassandraIndexingOptions.Builder builder) {
        m_maxBatchSize = builder.maxBatchSize;
        m_enableHierarchicalIndexing = builder.enableHierarchicalIndexing;
        m_indexUsingDefaultTerm = builder.indexUsingDefaultTerm;
        m_indexResourceTerms = builder.indexResourceTerms;
    }

    @Inject
    public CassandraIndexingOptions(@Named("search.hierarical-indexing") boolean enableHierarchicalIndexing) {
        this(new Builder().withHierarchicalIndexing(enableHierarchicalIndexing)); 
    }

    public boolean isHierarchicalIndexingEnabled() {
        return m_enableHierarchicalIndexing;
    }

    public boolean shouldIndexUsingDefaultTerm() {
        return m_indexUsingDefaultTerm;
    }

    public boolean shouldIndexResourceTerms() {
        return m_indexResourceTerms;
    }

    public int getMaxBatchSize() {
        return m_maxBatchSize;
    }
}

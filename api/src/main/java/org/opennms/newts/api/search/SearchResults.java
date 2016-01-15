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
package org.opennms.newts.api.search;


import static com.google.common.base.Preconditions.checkNotNull;


import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.opennms.newts.api.Resource;

import com.google.common.collect.Lists;


public class SearchResults implements Iterable<SearchResults.Result> {

    public static class Result {

        private final Resource m_resource;
        private final Collection<String> m_metrics;

        private Result(Resource resource, Collection<String> metrics) {
            m_resource = checkNotNull(resource, "resource argument");
            m_metrics = checkNotNull(metrics, "metrics argument");
        }

        public Resource getResource() {
            return m_resource;
        }

        public Collection<String> getMetrics() {
            return m_metrics;
        }

    }

    private final List<Result> m_results = Lists.newArrayList();

    public void addResult(Resource resource, Collection<String> metrics) {
        m_results.add(new Result(resource, metrics));
    }

    public int size() {
        return m_results.size();
    }

    @JsonIgnore
    public boolean isEmpty() { return m_results.isEmpty(); }
    
    @Override
    public Iterator<Result> iterator() {
        return m_results.iterator();
    }

}

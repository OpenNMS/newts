package org.opennms.newts.api.search;


import static com.google.common.base.Preconditions.checkNotNull;

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
    
    @Override
    public Iterator<Result> iterator() {
        return m_results.iterator();
    }

}

package org.opennms.newts.api.indexing;

import java.util.Collection;

import com.google.common.collect.Multimap;


public interface ResourceIndex {
    public ResourcePath search(String... path);
    public Collection<String> getMetrics(String resourceName);
    public void index(Multimap<String, String> metrics);
}

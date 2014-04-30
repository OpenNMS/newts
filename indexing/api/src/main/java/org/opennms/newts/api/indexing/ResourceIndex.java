package org.opennms.newts.api.indexing;

import com.google.common.collect.Multimap;


public interface ResourceIndex {
    public ResourcePath search(String... path);
    public String[] getMetrics(String resourceName);
    public void index(Multimap<String, String> metrics);
}

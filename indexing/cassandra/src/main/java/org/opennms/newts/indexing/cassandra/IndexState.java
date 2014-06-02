package org.opennms.newts.indexing.cassandra;


import com.google.common.collect.Multimap;


public interface IndexState {

    public void put(String resource, String metric);

    public void putAll(Multimap<String, String> metrics);

    public boolean exists(String resource, String metric);

}

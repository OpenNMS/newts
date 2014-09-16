package org.opennms.newts.api.search;

import java.util.Collection;

import org.opennms.newts.api.Sample;

public interface Indexer {
    public void update(Collection<Sample> samples);
}

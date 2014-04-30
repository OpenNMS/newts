package org.opennms.newts.api.indexing;

import java.util.Collection;

public interface ResourcePath {
    public Collection<ResourcePath> children();
    public ResourcePath parent();
    public String[] getMetrics();
}

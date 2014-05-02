package org.opennms.newts.api.indexing;


import java.util.Collection;

import com.google.common.base.Optional;


public interface ResourcePath {

    public String getName();

    public Collection<ResourcePath> getChildren();

    public Optional<ResourcePath> getParent();

    public Collection<String> getMetrics();

}

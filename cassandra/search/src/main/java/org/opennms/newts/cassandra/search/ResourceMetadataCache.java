package org.opennms.newts.cassandra.search;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;

import com.google.common.base.Optional;

public interface ResourceMetadataCache {
    void put(Context context, Resource resource, ResourceMetadata resourceMetadata);
    Optional<ResourceMetadata> get(Context context, Resource resource);
}

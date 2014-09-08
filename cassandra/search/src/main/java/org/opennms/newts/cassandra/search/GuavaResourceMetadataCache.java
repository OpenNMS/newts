package org.opennms.newts.cassandra.search;


import javax.inject.Inject;
import javax.inject.Named;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


public class GuavaResourceMetadataCache implements ResourceMetadataCache {

    private static final Joiner m_keyJoiner = Joiner.on(':');
    
    private final Cache<String, ResourceMetadata> m_cache;

    @Inject
    public GuavaResourceMetadataCache(@Named("search.rMetadata.maxCacheSize") long maxSize) {
        m_cache = CacheBuilder.newBuilder().maximumSize(maxSize).build();
    }

    @Override
    public Optional<ResourceMetadata> get(Context context, Resource resourceId) {
        ResourceMetadata r = m_cache.getIfPresent(key(context, resourceId));
        return (r != null) ? Optional.of(r) : Optional.<ResourceMetadata>absent();
    }

    private String key(Context context, Resource resource) {
        return m_keyJoiner.join(context.getId(), resource.getId());
    }

    @Override
    public void merge(Context context, Resource resource, ResourceMetadata rMetadata) {

        Optional<ResourceMetadata> o = get(context, resource);

        if (!o.isPresent()) {
            m_cache.put(key(context, resource), rMetadata);
            return;
        }

        o.get().merge(rMetadata);

    }

}

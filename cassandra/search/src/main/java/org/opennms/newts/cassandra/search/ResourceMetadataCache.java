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
package org.opennms.newts.cassandra.search;


import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;

import com.google.common.base.Optional;


public interface ResourceMetadataCache {
    /**
     * Merges the contents of a {@link ResourceMetadata} with the cached instance corresponding to
     * <code>context</code> and <code>resource</code>. If no such {@link ResourceMetadata} instance
     * exists, the supplied value is stored as-is.
     *
     * @param context
     *            application context of the resource metadata
     * @param resource
     *            the resource
     * @param rMetadata
     *            metadata associated with the resource
     */
    void merge(Context context, Resource resource, ResourceMetadata rMetadata);

    /**
     * Returns an {@link Optional} of the {@link ResourceMetadata} for <code>context</code> and
     * <code>resource</code>, or {@link Optional#absent()} if one does not exist.
     *
     * @param context
     *            application context of the resource metadata
     * @param resource
     *            the resource
     * @return resource metadata corresponding to <code>context</code> and <code>resource</code>
     */
    Optional<ResourceMetadata> get(Context context, Resource resource);
}

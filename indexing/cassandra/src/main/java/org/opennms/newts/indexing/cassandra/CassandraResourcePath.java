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
package org.opennms.newts.indexing.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;

import org.opennms.newts.api.indexing.ResourcePath;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;


public class CassandraResourcePath implements ResourcePath {

    private final Optional<ResourcePath> m_parent;
    private final String m_name;
    private final CassandraResourceIndex m_resourceIndex;

    private Optional<Collection<ResourcePath>> m_children = Optional.absent();
    private Optional<Collection<String>> m_metrics = Optional.absent();

    public CassandraResourcePath(String name, CassandraResourceIndex resourceIndex) {
        this(Optional.<ResourcePath> absent(), name, resourceIndex);
    }

    public CassandraResourcePath(Optional<ResourcePath> parent, String name, CassandraResourceIndex resourceIndex) {
        m_parent = checkNotNull(parent, "parent argument");
        m_name = checkNotNull(name, "name argument");
        m_resourceIndex = resourceIndex;//checkNotNull(resourceIndex, "resource index argument");
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public Collection<ResourcePath> getChildren() {
        if (m_children.isPresent()) return m_children.get();

        List<ResourcePath> children = Lists.newArrayList();

        for (String child : m_resourceIndex.getChildren(getFullyQualifiedName(this))) {
            children.add(new CassandraResourcePath(Optional.<ResourcePath>of(this), child, m_resourceIndex));
        }

        m_children = Optional.<Collection<ResourcePath>> of(children);

        return children;
    }

    @Override
    public Optional<ResourcePath> getParent() {
        return m_parent;
    }

    @Override
    public Collection<String> getMetrics() {
        if (m_metrics.isPresent()) return m_metrics.get();

        m_metrics = Optional.of(m_resourceIndex.getMetrics(getFullyQualifiedName(this)));

        return m_metrics.get();
    }

    @Override
    public String toString() {
        return getName();
    }

    private static String getFullyQualifiedName(ResourcePath path) {
        if (path.getParent().isPresent() && !isDummyRoot(path.getParent().get())) {
            return join(getFullyQualifiedName(path.getParent().get()), path.getName());
        }
        else {
            return path.getName();
        }
    }

    private static String join(String... args) {
        return Joiner.on(CassandraResourceIndex.DELIMITER).join(args);
    }

    private static boolean isDummyRoot(ResourcePath path) {
        return path.getName().equals(CassandraResourceIndex.ROOT_KEY);
    }

}

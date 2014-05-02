package org.opennms.newts.rest.indexing;

import java.util.Iterator;

import com.google.common.base.Splitter;

public class PathElements implements Iterable<String> {

    private final Iterable<String> m_components;

    public PathElements(String input) {
        m_components = Splitter.on('/').trimResults().omitEmptyStrings().split(input);
    }

    @Override
    public Iterator<String> iterator() {
        return m_components.iterator();
    }

}

package org.opennms.newts.api;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Map;

import com.google.common.base.Objects;


/**
 * A unique resource to associate a group of metrics to. Newts utilizes this structure to index the
 * resources of the samples it has seen, providing a means of search and discovery.
 * 
 * @author eevans
 */
public class Resource {

    public static final String DEFAULT_APPLICATION = "D";

    private final String m_application;
    private final String m_id;
    private final Map<String, String> m_attributes;

    /**
     * Creates a new {@link Resource} instance with the supplied resource ID, default application
     * ID, and an empty set of attributes.
     *
     * @param id
     *            the resource identifier.
     */
    public Resource(String id) {
        this(id, Collections.<String, String>emptyMap());
    }

    /**
     * Creates a new {@link Resource} instance with the supplied resource ID, attributes and the
     * default application ID.
     *
     * @param id
     *            the resource identifier.
     */
    public Resource(String id, Map<String, String> attributes) {
        this(DEFAULT_APPLICATION, id, attributes);
    }

    /**
     * Creates a new {@link Resource} with the supplied ID.
     *
     * @param application
     *            the parent application.
     * @param id
     *            resource identifier.
     * @param attributes
     *            attributes to associate with this resource.
     */
    public Resource(String application, String id, Map<String, String> attributes) {
        m_application = checkNotNull(application, "application argument");
        m_id = checkNotNull(id, "id argument");
        m_attributes = checkNotNull(attributes, "attributes argument");
    }

    /**
     * @return the application ID of this resource.
     */
    public String getApplication() {
        return m_application;
    }

    /**
     * @return the ID of this resource.
     */
    public String getId() {
        return m_id;
    }

    /**
     * @return the set of attributes for this resource.
     */
    public Map<String, String> getAttributes() {
        return m_attributes;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getApplication(), getId(), getAttributes());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Resource)) return false;
        return getApplication().equals(((Resource)o).getApplication()) && getId().equals(((Resource) o).getId());
    }

}

package org.opennms.newts.api;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;


/**
 * A unique resource to associate a group of metrics to.
 * <p>
 * Resources are capable of representing a hierarchical structure, like the directories of a
 * filesystem. Newts utilizes this structure to index the resources of the samples it has seen,
 * providing a means of search and discovery.
 * </p>
 * 
 * @author eevans
 */
public class Resource {

    /**
     * The resource delimiter character.
     */
    public static final char SEPERATOR_CHAR = '/';

    /**
     * The resource delimiter represented as a {@link String}. It contains a single character,
     * namely {@link SEPERATOR_CHAR}.
     */
    public static final String SEPERATOR = "" + SEPERATOR_CHAR;

    private static final Splitter s_splitter = Splitter.on(SEPERATOR_CHAR).omitEmptyStrings();
    private static final Joiner s_joiner = Joiner.on(SEPERATOR_CHAR);

    private final Optional<Resource> m_parent;
    private final String m_name;
    private final Map<String, String> m_attributes;

    /**
     * Creates a new {@link Resource} instance with the supplied resource ID, and an empty set of
     * attributes.
     *
     * @param id
     *            the resource identifier.
     */
    public Resource(String id) {
        this(id, Collections.<String, String> emptyMap());
    }

    /**
     * Creates a new {@link Resource} instance with the supplied resource ID, and attributes.
     *
     * @param id
     *            the resource identifier.
     * @param attributes
     *            attributes to associate with this resource.
     */
    public Resource(String id, Map<String, String> attributes) {
        checkNotNull(id, "id argument");
        checkNotNull(attributes, "attributes argument");

        Optional<Resource> parent = Optional.<Resource> absent();
        List<String> names = split(id);

        for (int i = 0; i < (names.size() - 1); i++) {
            parent = Optional.of(new Resource(parent, names.get(i)));
        }

        m_parent = parent;
        m_name = names.get(names.size() - 1);
        m_attributes = attributes;

    }

    /**
     * Creates a new {@link Resource} instance with the supplied parent {@link Resource}, and child
     * name. The resource attributes will be initialized to an empty {@link Map}.
     * 
     * @param parent
     *            the parent resource.
     * @param name
     *            the child name.
     */
    public Resource(Optional<Resource> parent, String name) {
        this(parent, name, Collections.<String, String> emptyMap());
    }

    /**
     * Creates a new {@link Resource} instance with the supplied parent {@link Resource}, and child
     * name. The resource attributes will be initialized to an empty {@link Map}.
     * 
     * @param parent
     *            the parent resource.
     * @param name
     *            the child name.
     * @param attributes
     *            attributes to associate with this resource.
     */
    public Resource(Optional<Resource> parent, String name, Map<String, String> attributes) {
        m_parent = checkNotNull(parent, "parent argument");
        m_name = checkNotNull(name, "name argument");
        m_attributes = checkNotNull(attributes, "attributes argument");
    }

    /**
     * @return the fully qualified ID of this resource.
     */
    public String getId() {
        return String.format("/%s", join(getAncestorNames()));
    }

    /**
     * @return the parent of this resource.
     */
    public Optional<Resource> getParent() {
        return m_parent;
    }

    /**
     * @return the simple (read: short) name of this resource
     */
    public String getName() {
        return m_name;
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
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Resource)) return false;
        return getId().equals(((Resource) o).getId());
    }

    private List<Resource> getAncestorResources() {

        Resource parent, child = this;
        List<Resource> adjacent = Lists.newArrayList(child);

        while (child.getParent().isPresent()) {
            parent = child.getParent().get();
            adjacent.add(parent);
            child = parent;
        }

        Collections.reverse(adjacent);

        return adjacent;
    }

    private List<String> getAncestorNames() {
        return Lists.transform(getAncestorResources(), new Function<Resource, String>() {

            @Override
            public String apply(Resource input) {
                return input.getName();
            }
        });
    }

    private static String scrub(String id) {
        return id;
    }

    private static List<String> split(String id) {
        return s_splitter.splitToList(scrub(id));
    }

    private static String join(Iterable<String> names) {
        return s_joiner.join(names);
    }

}

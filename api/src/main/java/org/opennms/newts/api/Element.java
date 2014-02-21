package org.opennms.newts.api;

/**
 * An element of a {@link Results} matrix.
 * 
 * @author eevans
 *
 * @param <T>
 */
public interface Element<T> {

    Timestamp getTimestamp();

    String getResource();

    String getName();

    T getValue();

}

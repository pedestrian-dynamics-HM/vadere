package org.vadere.simulator.projects.dataprocessing.datakey;

/**
 * Base class for data keys used for data processors. Subclasses must implement
 * {@code Comparable} (for correct order in the output file) and
 * {@link Object#hashCode()} and {@link Object#equals(Object)} (for the data
 * processor's map and internals).
 */
public interface DataKey<T extends DataKey<?>> extends Comparable<T> {
}

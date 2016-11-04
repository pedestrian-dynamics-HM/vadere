package org.vadere.state.attributes;

/**
 * Abstract class for all static simulation attributes. Provides reflection
 * based methods to convert the fields and values of the Attributes classes from
 * and to key-value store.
 * 
 * Implementations must provide a no-arg default contstructor (either implicitly
 * or explicitly) to enable deserialization from JSON.
 */
public abstract class Attributes {
}

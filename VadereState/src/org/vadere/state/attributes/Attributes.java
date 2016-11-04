package org.vadere.state.attributes;

import org.apache.commons.attributes.DefaultSealable;

/**
 * Abstract base class for all static simulation attributes.
 * 
 * Implementations must provide a no-arg default contstructor (either implicitly
 * or explicitly) to enable deserialization from JSON.
 *
 * Attributes are "static" parameters i.e. they are immutable while a simulation
 * is running. This is implemented by deriving from the
 * {@link org.apache.commons.attributes.Sealable} interface. Attributes can have
 * setters, but the setters must call the {@code checkSealed()} method before
 * changing a value! In addition, if an attributes class contains other
 * attributes classes as fields, it must override {@link #seal()} to also seal
 * these objects. All other fields must be immutable (e.g. String, Double,
 * VPoint,...).
 * 
 */
public abstract class Attributes extends DefaultSealable {
}

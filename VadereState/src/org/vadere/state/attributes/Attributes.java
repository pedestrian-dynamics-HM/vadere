package org.vadere.state.attributes;


import org.vadere.util.reflection.VadereAttribute;

import java.io.IOException;

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
 * The standard clone method makes a flat copy. This is enough if the subclass
 * only triangleContains immutable fields. If the subclass triangleContains other Attributes
 * objects, it must implement a copy constructor and override {@link #clone()}
 * to make a deep copy.
 * 
 */
@VadereAttribute
public abstract class Attributes extends DefaultSealable implements Cloneable {
	@VadereAttribute(exclude = true)
	/** Used for default ID values of some scenario elements. */
	public static final int ID_NOT_SET = -1;

	public Attributes() {}
	
	/**
	 * Standard flat copy of attributes. The flat copy is only sufficient (as
	 * noted above) if all fields are immutable.
	 */
	@Override
	public Attributes clone() {
		try {
			return (Attributes) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("This should never happen because the base class is Cloneable.", e);
		}
	}

    public void check() throws IOException {
    }
}

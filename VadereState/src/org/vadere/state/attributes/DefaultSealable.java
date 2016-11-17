package org.vadere.state.attributes;

import org.apache.commons.attributes.Sealable;

/**
 * Our implementation of {@link Sealable}.
 * The {@code sealed} flag shall not be serialized.
 * 
 * @author Jakob Schöttl
 *
 */
public class DefaultSealable implements Sealable {
	
	/** Should not be serialized. */
	private transient boolean sealed;

	/**
	 * Apache Commons' DefaultSealable throws an exception if the object is already sealed.
	 */
	@Override
	public void seal() {
		// Attributes currently are sealed right before the simulation starts.
		// Throwing an exception when they are already sealed makes a second simulation fail.
		// Solutions (other than not throwing a exception) are:
		// - Seal as soon as the objects are loaded from JSON.
		// - Copy all attributes before starting a simulation.
		//   This does not make sense, since the attributes are read-only anyway.

//		if (sealed)
//			throw new IllegalStateException("Object is already sealed.");
		sealed = true;
	}
	
	protected void checkSealed() {
		if (sealed)
			throw new IllegalStateException("Object is sealed.");
	}

}

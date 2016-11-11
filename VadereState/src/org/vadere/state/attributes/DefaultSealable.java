package org.vadere.state.attributes;

import org.apache.commons.attributes.Sealable;

public class DefaultSealable implements Sealable {
	
	/** Should not be serialized. */
	private transient boolean sealed;

	@Override
	public void seal() {
		if (sealed)
			throw new IllegalStateException("Object is already sealed.");
		sealed = true;
	}
	
	protected void checkSealed() {
		if (sealed)
			throw new IllegalStateException("Object is sealed.");
	}

}

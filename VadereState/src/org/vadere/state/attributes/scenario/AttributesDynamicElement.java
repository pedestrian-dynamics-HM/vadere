package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.Attributes;

public class AttributesDynamicElement extends Attributes {
	private int id;

	public AttributesDynamicElement() {
		this(-1);
	}

	public AttributesDynamicElement(final int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		checkSealed();
		this.id = id;
	}
}

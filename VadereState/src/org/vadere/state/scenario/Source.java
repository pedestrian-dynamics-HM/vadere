package org.vadere.state.scenario;

import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VShape;

public class Source extends ScenarioElement<AttributesSource> {

	public Source(AttributesSource attributes) {
		this.attributes = attributes;
	}

	@Override
	public void setShape(VShape newShape) {
		attributes.setShape(newShape);
	}

	@Override
	public VShape getShape() {
		return attributes.getShape();
	}

	@Override
	public int getId() {
		return attributes.getId();
	}

	@Override
	public void setId(int id) {
		attributes.setId(id);
	}

	public double getStartTime() {
		return attributes.getSpawnerAttributes().getConstraintsTimeStart();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Source)) {
			return false;
		}
		Source other = (Source) obj;
		if (attributes == null) {
			return other.attributes == null;
		} else return this.attributes.equals(other.attributes);
	}

	@Override
	public ScenarioElementType getType() {
		return ScenarioElementType.SOURCE;
	}

	@Override
	public Source clone() {
		return new Source((AttributesSource) attributes.clone());
	}
}

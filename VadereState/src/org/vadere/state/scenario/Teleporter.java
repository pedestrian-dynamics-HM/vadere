package org.vadere.state.scenario;

import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesTeleporter;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

public class Teleporter extends ScenarioElement {

	private AttributesTeleporter attributes;

	public Teleporter(AttributesTeleporter attributes) {
		this.attributes = attributes;
	}

	public Vector2D getTeleporterShift() {
		return this.attributes.getTeleporterShift();
	}

	public VPoint getTeleporterPosition() {
		return this.attributes.getTeleporterPosition();
	}

	@Override
	public VShape getShape() {
		throw new UnsupportedOperationException("A teleporter does not have a shape.");
	}

	@Override
	public void setShape(VShape newShape) {
		throw new UnsupportedOperationException("A teleporter does not have a shape.");
	}

	@Override
	public int getId() {
		return -1;
	}

	@Override
	public void setId(int id) {
		// do nothing?
	}

	@Override
	public AttributesTeleporter getAttributes() {
		return attributes;
	}

	@Override
	public void setAttributes(Attributes attributes) {
		this.attributes = (AttributesTeleporter) attributes;
	}

	@Override
	public ScenarioElementType getType() {
		return ScenarioElementType.TELEPORTER;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Teleporter that = (Teleporter) o;

		if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return attributes != null ? attributes.hashCode() : 0;
	}

	@Override
	public Teleporter clone() {
		return new Teleporter((AttributesTeleporter) attributes.clone());
	}

}

package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Objects;

public class AttributesMeasurementArea extends AttributesEmbedShape {

	private VShape shape;
	private int id;

	public AttributesMeasurementArea(){};

	public AttributesMeasurementArea(int id) {
		this.id = id;
	}

	public AttributesMeasurementArea(int id, VShape shape) {
		this.shape = shape;
		this.id = id;
	}

	@Override
	public void setShape(VShape shape) {
		this.shape = shape;
	}

	@Override
	public VShape getShape() {
		return shape;
	}

	public int getId() {
		checkSealed();
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AttributesMeasurementArea that = (AttributesMeasurementArea) o;
		return id == that.id &&
				shape.equals(that.shape);
	}

	@Override
	public int hashCode() {
		int result = shape != null ? shape.hashCode() : 0;
		result = 31 * result + id;
		return result;
	}
}

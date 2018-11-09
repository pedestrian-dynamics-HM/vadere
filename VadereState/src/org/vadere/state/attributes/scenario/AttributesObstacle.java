package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.util.geometry.shapes.VShape;

public class AttributesObstacle extends AttributesEmbedShape {

	private VShape shape;
	private int id;

	public AttributesObstacle() {}

	public AttributesObstacle(int id) {
		this.id = id;
	}

	public AttributesObstacle(int id, VShape shape) {
		this(id);
		this.shape = shape;
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
		return id;
	}

	public void setId(int id) {
		checkSealed();
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AttributesObstacle that = (AttributesObstacle) o;

		if (id != that.id) return false;
		return shape != null ? shape.equals(that.shape) : that.shape == null;
	}

	@Override
	public int hashCode() {
		int result = shape != null ? shape.hashCode() : 0;
		result = 31 * result + id;
		return result;
	}
}
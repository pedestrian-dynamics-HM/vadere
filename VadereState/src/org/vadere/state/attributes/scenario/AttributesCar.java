package org.vadere.state.attributes.scenario;

import org.vadere.util.geometry.shapes.Vector2D;

public class AttributesCar extends AttributesAgent {

	private double length = 4.5;
	private double width = 1.7;
	private Vector2D direction = new Vector2D(1, 0);


	public AttributesCar(final AttributesCar other, final int id) {
		super(other, id);
		this.length = other.length;
		this.width = other.width;
		this.direction = other.direction;
	}

	public AttributesCar() {
		this(-1);
	}

	public AttributesCar(final int id) {
		super(id);
	}

	// Getters

	public double getLength() {
		return length;
	}

	public double getWidth() {
		return width;
	}

	public Vector2D getDirection() {
		return direction;
	}

	public void setDirection(Vector2D direction) {
		checkSealed();
		this.direction = direction;
	}

	@Override
	public double getRadius() {
		if (width >= length) {
			return width;
		} else {
			return length;
		}
	}
}


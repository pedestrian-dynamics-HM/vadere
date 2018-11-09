package org.vadere.state.scenario;

import java.util.Random;

import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesCar;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;

public class Car extends Agent implements Comparable<Car> {

	private AttributesCar attributesCar;

	public Car(AttributesCar attributesCar, Random random) {
		super(attributesCar, random);

		this.attributesCar = attributesCar;
		setPosition(new VPoint(0, 0));
		setVelocity(new Vector2D(0, 0));
		// this.targetIds = new LinkedList<>();
	}

	@Override
	public int compareTo(Car o) {
		Double thisPos = new Double(getPosition().getX());
		Double othPos = new Double(o.getPosition().getX());

		if (attributesCar.getDirection().getX() >= 0) {
			return -1 * thisPos.compareTo(othPos);
		} else {
			return thisPos.compareTo(othPos);
		}
	}

	public AttributesCar getCarAttributes() {
		return attributesCar;
	}

	@Override
	public VPolygon getShape() {

		// Rectangle with the Attributes of a Car
		VRectangle rect = new VRectangle(getPosition().getX() - attributesCar.getLength(),
				getPosition().getY() - attributesCar.getWidth() / 2, attributesCar.getLength(),
				attributesCar.getWidth());
		VPolygon poly = new VPolygon(rect);

		// turn the car in the driving direction
		double angle = this.getVelocity().angleToZero();
		poly = poly.rotate(getPosition(), angle);
		return poly;
	}

	@Override
	public ScenarioElementType getType() {
		return ScenarioElementType.CAR;
	}

	@Override
	public AttributesCar getAttributes() {
		return this.attributesCar;
	}

	public void setAttributes(Attributes attributes) {
		this.attributesCar = (AttributesCar) attributes;
	}

	@Override
	public int getId() {
		return attributesCar.getId();
	}

	@Override
	public Car clone() {
		throw new RuntimeException("clone is not supported for Car; it seems hard to implement.");
		// return new Car(attributesCar, new Random());
		// TODO get random from super class instead of creating a new one
		// TODO attributesAgent in super class must be copied as well
	}

}

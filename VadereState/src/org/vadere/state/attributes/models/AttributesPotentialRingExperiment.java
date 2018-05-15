package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.util.geometry.shapes.VPoint;

@ModelAttributeClass
public class AttributesPotentialRingExperiment extends Attributes {

	private VPoint center = new VPoint(5, 5);
	private double radius1 = 2;
	private double radius2 = 4.5;
	private double pedestrianTrajectory1 = 2.4;
	private double pedestrianTrajectory2 = 4.1;
	private double allowedTrajectoryWidth = 0.1;
	private double pedestrianRadius = 0.5;

	public VPoint getCenter() {
		return center;
	}

	public double getRadius1() {
		return radius1;
	}

	public double getRadius2() {
		return radius2;
	}

	public double getPedestrianTrajectory1() {
		return pedestrianTrajectory1;
	}

	public double getPedestrianTrajectory2() {
		return pedestrianTrajectory2;
	}

	public double getAllowedTrajectoryWidth() {
		return allowedTrajectoryWidth;
	}

	public double getPedestrianRadius() {
		return pedestrianRadius;
	}
}

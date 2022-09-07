package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.util.geometry.shapes.VPoint;

@ModelAttributeClass
public class AttributesPotentialRingExperiment extends Attributes {

	private final VPoint center = new VPoint(5, 5);
	private final double radius1 = 2;
	private final double radius2 = 4.5;
	private final double pedestrianTrajectory1 = 2.4;
	private final double pedestrianTrajectory2 = 4.1;
	private final double allowedTrajectoryWidth = 0.1;
	private final double pedestrianRadius = 0.5;

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

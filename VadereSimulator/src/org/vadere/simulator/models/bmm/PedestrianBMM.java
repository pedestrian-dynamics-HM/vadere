package org.vadere.simulator.models.bmm;

import java.util.Random;

import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.attributes.models.AttributesBMM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;

public class PedestrianBMM extends Pedestrian {

	private final Topography topography;
	private final AttributesBMM attributesBMM;
	private final PedestrianBHM pedestrianBHM;

	private VPoint nextStepPosition;
	private VPoint lastMotionPosition;
	private VPoint nextMotionPosition;
	private VPoint nextVelocity;
	private VPoint nextAcceleration;
	private double stepSum;

	private boolean isInitialized;

	public PedestrianBMM(VPoint position, Topography topography, AttributesAgent attributesPedestrian,
			AttributesBMM attributesBMM, AttributesBHM attributesBHM, Random random) {

		super(attributesPedestrian, random);
		super.setPosition(position);

		this.topography = topography;
		this.attributesBMM = attributesBMM;
		this.pedestrianBHM = new PedestrianBHM(topography, attributesPedestrian, attributesBHM, random);
		this.isInitialized = false;
	}

	void intialize() {
		pedestrianBHM.setTargets(getTargets());
		reset();
		this.isInitialized = true;
	}

	void reset() {
		this.nextStepPosition = getPosition();
		this.lastMotionPosition = getPosition();
		this.nextMotionPosition = getPosition();
		this.nextVelocity = new VPoint(0, 0);
		this.nextAcceleration = new VPoint(0, 0);
		this.stepSum = 0;
	}

	void update(double simTime, double deltaTime) {

		if (!isInitialized) {
			intialize();
			updateStepPosition(simTime);
		} else if (!attributesBMM.isStepwiseDecisions() ||
				pedestrianBHM.getStepLength() < stepSum ||
				getVelocity().distanceToOrigin() < GeometryUtils.DOUBLE_EPS) {
			updateStepPosition(simTime);
		}

		updateAcceleration(simTime, deltaTime);
		updateVelocity(simTime, deltaTime);
		updatePosition(simTime, deltaTime);
	}

	void updatePosition(double simTime, double deltaTime) {
		this.nextMotionPosition = getPosition().add(nextVelocity.scalarMultiply(deltaTime));

		// if next position collides with wall or other pedestrian, remain at current position
		if (pedestrianBHM.collidesWithObstacle(nextMotionPosition) ||
				pedestrianBHM.collidesWithPedestrian(nextMotionPosition, 0)) {
			this.nextMotionPosition = getPosition();
		}
	}

	void updateVelocity(double simTime, double deltaTime) {
		this.nextVelocity = getVelocity().add(nextAcceleration.scalarMultiply(deltaTime));
	}

	void updateAcceleration(double simTime, double deltaTime) {

		VPoint preferredVelocity = nextStepPosition.subtract(getPosition()).normZeroSafe();
		preferredVelocity = preferredVelocity.scalarMultiply(getFreeFlowSpeed());

		VPoint acceleration = preferredVelocity.subtract(getVelocity());

		this.nextAcceleration = acceleration.scalarMultiply(attributesBMM.getAcceleration());
	}

	void move(double simTime, double deltaTime) {
		VPoint currentPosition = getPosition();

		this.lastMotionPosition = currentPosition;
		setPosition(nextMotionPosition);

		Vector2D motionStep = new Vector2D(nextMotionPosition.x - currentPosition.x,
				nextMotionPosition.y - currentPosition.y);

		if (deltaTime < GeometryUtils.DOUBLE_EPS) {
			setVelocity(new Vector2D(0, 0));
		} else {
			// compute velocity by forward difference
			setVelocity(motionStep.multiply(1.0 / deltaTime));
		}

		this.stepSum = stepSum + motionStep.distanceToOrigin();
	}

	public void reverseCollisions() {
		// if next position collides with wall or other pedestrian, remain at current position
		if (pedestrianBHM.collidesWithObstacle(nextMotionPosition) ||
				pedestrianBHM.collidesWithPedestrian(nextMotionPosition, 0)) {
			setPosition(lastMotionPosition);
			reset();
		}
	}

	private void updateStepPosition(double simTime) {
		pedestrianBHM.setPosition(this.getPosition());
		pedestrianBHM.update(simTime);
		this.nextStepPosition = pedestrianBHM.getPosition();
		this.stepSum = 0;
	}


}

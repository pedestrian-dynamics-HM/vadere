package org.vadere.simulator.models.bhm;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

public class PedestrianBHM extends Pedestrian {

	private static Logger logger = LogManager.getLogger(PedestrianBHM.class);

	private final Random random;
	private final AttributesBHM attributesBHM;
	private final Topography topography;

	private final double stepLength;

	private double durationNextStep;
	private double timeOfNextStep;
	private VPoint nextPosition;
	private VPoint lastPosition;
	private VPoint targetDirection;

	private final Navigation navigation;
	private final List<DirectionAddend> directionAddends;

	protected int action;

	private boolean evadesTangentially;
	private boolean evadesSideways;
	private int remainCounter;

	public PedestrianBHM(Topography topography, AttributesAgent attributesPedestrian,
			AttributesBHM attributesBHM, Random random) {
		super(attributesPedestrian, random);

		this.random = random;
		this.attributesBHM = attributesBHM;
		this.topography = topography;

		this.setVelocity(new Vector2D(0, 0));

		double stepDeviation = 0;

		if (attributesBHM.isStepLengthDeviation()) {
			stepDeviation = random.nextGaussian() * attributesBHM.getStepLengthSD();
		}

		this.stepLength = attributesBHM.getStepLengthIntercept() + stepDeviation +
				attributesBHM.getStepLengthSlopeSpeed() * getFreeFlowSpeed();

		this.directionAddends = new LinkedList<>();


		// model building ...

		if (attributesBHM.isNavigationCluster()) {
			this.navigation = new NavigationCluster(this, topography, random);
			if (attributesBHM.isNavigationFollower()) {
				logger.warn("Only one navigation heuristic can be chosen."
						+ "Choosing cluster navigation.");
			}
		} else if (attributesBHM.isNavigationFollower()) {
			this.navigation = new NavigationFollower(this, topography, random);
			if (attributesBHM.isNavigationCluster()) {
				logger.warn("Only one navigation heuristic can be chosen."
						+ "Choosing follower navigation.");
			}
		} else {
			this.navigation = new NavigationProximity(this, random);
		}

		if (attributesBHM.isDirectionWallDistance()) {
			directionAddends.add(new DirectionAddendObstacle(this));
		}

		setNextTargetListIndex(0);

		setEvasionStrategy();
	}

	private void setEvasionStrategy() {

		if (attributesBHM.isSwitchBehaviour()) {
			if (remainCounter > attributesBHM.getAdaptiveBehaviourStepsRemained().get(0)) {
				this.evadesTangentially = true;
			}
			if (remainCounter > attributesBHM.getAdaptiveBehaviourStepsRemained().get(1)) {
				this.evadesSideways = true;
			}
		} else {
			this.evadesTangentially = false;
			this.evadesSideways = false;

			if (attributesBHM.isDifferentBehaviour()) {
				int evasionChoice =
						UtilsBHM.randomChoice(attributesBHM.getDifferentEvasionBehaviourPercentage(), random);
				if (evasionChoice > 1) {
					this.evadesTangentially = true;
				}
				if (evasionChoice > 2) {
					this.evadesSideways = true;
				}
			} else if (attributesBHM.isAdaptiveBehaviourDensity()) {
				if (remainCounter > attributesBHM.getAdaptiveBehaviourStepsRemained().get(0)) {
					this.evadesTangentially = true;
				}
				if (remainCounter > attributesBHM.getAdaptiveBehaviourStepsRemained().get(1)) {
					this.evadesSideways = true;
				}
			} else {
				if (attributesBHM.isTangentialEvasion()) {
					this.evadesTangentially = true;
				}
				if (attributesBHM.isSidewaysEvasion()) {
					this.evadesSideways = true;
				}
			}
		}
	}

	/**
	 * Updates the pedestrian. Changes the object's state!
	 */
	public void update(double currentTimeInSec) {

		if (attributesBHM.isVaryingBehaviour()) {
			setEvasionStrategy();
		}

		// for the first step after creation, timeOfNextStep has to be initialized
		if (getTimeOfNextStep() == 0) {
			this.timeOfNextStep = currentTimeInSec;
		}

		this.durationNextStep = this.stepLength / getFreeFlowSpeed();

		// This has to happen here! The call has side effects on navigation!
		updateTargetDirection();

		this.nextPosition = navigation.getNavigationPosition();

		makeStep();

		this.timeOfNextStep = timeOfNextStep + durationNextStep;
	}

	/**
	 * Realizes the next step by setting member variable position from super class Pedestrian,
	 * and setting member variable lastPosition=position. Also updates super member velocity.
	 * 
	 * This method sets the member variables and thus has side effects!
	 */
	public void makeStep() {
		VPoint currentPosition = getPosition();

		// note that velocity is only set when agent has actually moved
		if (nextPosition.equals(currentPosition)) {
			this.remainCounter++;
			setVelocity(new Vector2D(0, 0));
		} else {
			this.lastPosition = getPosition();
			setPosition(nextPosition);

			// compute velocity by forward difference
			setVelocity(new Vector2D(nextPosition.x - currentPosition.x,
					nextPosition.y - currentPosition.y).multiply(1.0 / durationNextStep));
			this.remainCounter = 0;
		}
	}

	VPoint stepAwayFromCollision(final Pedestrian collisionPed) {
		return getPosition().subtract(collisionPed.getPosition()).norm().scalarMultiply(stepLength).add(getPosition());
	}

	VPoint makeSmallerStep(final VPoint position, final Pedestrian collisionPed) {

		VPoint result;

		int smallStepResolution = attributesBHM.getSmallStepResolution();
		double stepFraction = 1.0 / smallStepResolution;

		VPoint direction = this.getPosition().subtract(position);

		for (int i = smallStepResolution; i > 0; i--) {

			result = getPosition().add(direction.scalarMultiply(
					stepLength * stepFraction * smallStepResolution));

			if (!collidesWithPedestrian(result, 0) && !collidesWithObstacle(result)) {
				return result;
			}
		}

		return getPosition();
	}


	// target direction methods...

	VPoint computeTargetStep() {
		return UtilsBHM.getTargetStep(this, this.getPosition(), this.getTargetDirection());
	}

	/**
	 * Updates the target direction, considering DirectionAddends.
	 * 
	 * This method sets the member variable targetDirection and thus has side effects!
	 * Therefore this method should always stay private.
	 */
	private void updateTargetDirection() {

		targetDirection = VPoint.ZERO;

		if (attributesBHM.isReconsiderOldTargets()) {
			reconsiderOldTargets();
		}

		if (hasNextTarget()) {
			VShape targetShape = topography.getTarget(getNextTargetId()).getShape();
			if (!targetShape.contains(getPosition())) {
				VPoint targetPoint = targetShape.closestPoint(getPosition());
				targetDirection = targetPoint.subtract(getPosition()).norm();

				for (DirectionAddend da : directionAddends) {
					targetDirection = targetDirection.add(da.getDirectionAddend());
				}

				if (!targetDirection.equals(VPoint.ZERO)) {
					targetDirection = targetDirection.norm();
				}
			}
		}
	}

	/**
	 * Set the last target if beyond a certain threshold.
	 */
	private void reconsiderOldTargets() {
		if (getTargets().size() > 1 && getNextTargetListIndex() > 0) {
			int lastTargetListIndex = getNextTargetListIndex() - 1;
			Target lastTarget = topography.getTarget(getTargets().get(lastTargetListIndex));

			if (!lastTarget.getShape().contains(getPosition())) {

				if (getPosition().getX() > attributesBHM.getTargetThresholdX() ||
						getPosition().getY() > attributesBHM.getTargetThresholdY()) {

					setNextTargetListIndex(lastTargetListIndex);
				}
			}
		}
	}

	VPoint computeMovementProjection() {
		return getPosition().add(getTargetDirection().scalarMultiply(
				stepLength * attributesBHM.getPlannedStepsAhead()));
	}


	boolean collides(VPoint position) {
		return collidesWithPedestrian(position, 0) || collidesWithObstacle(position);
	}

	// pedestrian collision methods...

	/**
	 * This does not check collisions on the path, just collisions with position!
	 */
	public boolean collidesWithPedestrian(VPoint position, double spaceToKeep) {

		for (Pedestrian other : topography.getElements(Pedestrian.class)) {
			if (other.getId() != getId()) {

				double distance = position.distance(other.getPosition()) -
						other.getRadius() - getRadius() - spaceToKeep;

				if (distance < 0) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Check collisions on the path.
	 */
	boolean collidesWithPedestrianOnPath(VPoint position) {
		Pedestrian collision = findCollisionPedestrian(position, true);

		return collision != null;
	}

	/**
	 * If findAny is false, return the first collision on the path to position.
	 * If findAny is true, return the first collision that was found (could be any).
	 */
	Pedestrian findCollisionPedestrian(VPoint position, boolean findAny) {

		Pedestrian result = null;
		double minDistance = Double.MAX_VALUE;

		VLine stepLine = new VLine(getPosition(), position);

		for (Pedestrian other : topography.getElements(Pedestrian.class)) {
			if (other.getId() != getId()) {

				double distance = stepLine.distance(other.getPosition()) -
						other.getRadius() - getRadius() - attributesBHM.getSpaceToKeep();

				if (distance < 0) {

					if (findAny) {
						return other;

					} else if (!attributesBHM.isOnlyEvadeContraFlow() ||
							(UtilsBHM.angleBetweenTargetDirection(this, other) > attributesBHM
									.getOnlyEvadeContraFlowAngle())) {

						distance = this.getPosition().distance(other.getPosition()) - other.getRadius();

						if (distance < minDistance) {
							result = other;
							minDistance = distance;
						}
					}
				}
			}
		}

		return result;
	}


	// obstacle collision methods...

	/**
	 * This does not check collisions on the path, just collisions with position!
	 */
	public boolean collidesWithObstacle(VPoint position) {
		if (detectObstacleProximity(position, this.getRadius()).size() == 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * This does not check collisions on the path, just collisions with position!
	 */
	List<Obstacle> detectObstacleProximity(VPoint position, double proximity) {

		Collection<Obstacle> obstacles = topography.getObstacles();
		List<Obstacle> result = new LinkedList<>();

		for (Obstacle obstacle : obstacles) {
			if (obstacle.getShape().distance(position) < proximity) {
				result.add(obstacle);
			}
		}

		return result;
	}


	// Java nuisance...

	public double getTimeOfNextStep() {
		return timeOfNextStep;
	}

	public double getDurationNextStep() {
		return durationNextStep;
	}

	public VPoint getLastPosition() {
		return lastPosition;
	}

	public double getStepLength() {
		return stepLength;
	}

	public VPoint getTargetDirection() {
		return targetDirection;
	}

	public AttributesBHM getAttributesBHM() {
		return this.attributesBHM;
	}

	public int getBehaviour() {
		return this.action;
	}

	public boolean evadesTangentially() {
		return this.evadesTangentially;
	}

	public boolean evadesSideways() {
		return this.evadesSideways;
	}

	public int getHeuristic() {
		if (evadesSideways) {
			return 3;
		} else if (evadesTangentially) {
			return 2;
		} else {
			return 1;
		}
	}
}

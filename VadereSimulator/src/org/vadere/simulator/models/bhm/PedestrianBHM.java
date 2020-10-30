package org.vadere.simulator.models.bhm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.simulator.models.bhm.helpers.navigation.INavigation;
import org.vadere.simulator.models.bhm.helpers.navigation.NavigationBuilder;
import org.vadere.simulator.models.bhm.helpers.navigation.NavigationEvasion;
import org.vadere.simulator.models.bhm.helpers.targetdirection.*;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.utils.topography.TopographyHelper;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.util.*;

public class PedestrianBHM extends Pedestrian {

	private static Logger logger = Logger.getLogger(PedestrianBHM.class);

	private final transient Random random;
	private final AttributesBHM attributesBHM;
	private final transient Topography topography;

	private final double stepLength;

	private double durationNextStep;
	private double timeOfNextStep;
	private VPoint nextPosition;
	private VPoint lastPosition;
	private VPoint targetDirection;

	private final transient INavigation navigation;
	private final transient List<DirectionAddend> directionAddends;

	public int action;

	private boolean evadesTangentially;
	private boolean evadesSideways;
	private int remainCounter;
	private transient @Nullable IPotentialFieldTarget potentialFieldTarget;
	private transient TargetDirection targetDirectionStrategy;

	public PedestrianBHM(Topography topography, AttributesAgent attributesPedestrian,
	                     AttributesBHM attributesBHM, Random random) {
		this(topography, attributesPedestrian, attributesBHM, random, null);
	}

	public PedestrianBHM(Topography topography, AttributesAgent attributesPedestrian,
			AttributesBHM attributesBHM, Random random, @Nullable IPotentialFieldTarget potentialFieldTarget) {
		super(attributesPedestrian, random);
		this.potentialFieldTarget = potentialFieldTarget;
		this.random = random;
		this.attributesBHM = attributesBHM;
		this.topography = topography;
		this.timeOfNextStep = INVALID_NEXT_EVENT_TIME;

		this.setVelocity(new Vector2D(0, 0));

		double stepDeviation = 0;

		if (attributesBHM.isStepLengthDeviation()) {
			stepDeviation = random.nextGaussian() * attributesBHM.getStepLengthSD();
		}

		this.stepLength = attributesBHM.getStepLengthIntercept() + stepDeviation +
				attributesBHM.getStepLengthSlopeSpeed() * getFreeFlowSpeed();

		this.directionAddends = new LinkedList<>();


		// model building ...
		String navigationModel = attributesBHM.getNavigationModel();
		this.navigation = NavigationBuilder.instantiateModel(navigationModel, this, topography, random);

		if (attributesBHM.isDirectionWallDistance()) {
			directionAddends.add(new DirectionAddendObstacle(this));
		}

		setNextTargetListIndex(0);
		setEvasionStrategy();
		setTargetDirectionStrategy();
	}

	private void setTargetDirectionStrategy() {
		if(isPotentialFieldInUse()) {
			TargetDirection base = new TargetDirectionGeoGradient(this, potentialFieldTarget);
			targetDirectionStrategy = new TargetDirectionClose(this, potentialFieldTarget, base);
		} else {
			targetDirectionStrategy = new TargetDirectionEuclidean(this);
		}
	}

	public boolean isPotentialFieldInUse() {
		return potentialFieldTarget != null;
	}

	public IPotentialFieldTarget getPotentialFieldTarget() { return potentialFieldTarget; }

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
		if (getTimeOfNextStep() == INVALID_NEXT_EVENT_TIME) {
			timeOfNextStep = currentTimeInSec;
			return;
		}

		durationNextStep = stepLength / getFreeFlowSpeed();

		double startTimeStep = timeOfNextStep;
		double endTimeStep = timeOfNextStep + durationNextStep;
		timeOfNextStep = endTimeStep;

		SelfCategory selfCategory = getSelfCategory();

		VPoint position = getPosition();
		if (selfCategory == SelfCategory.TARGET_ORIENTED) {
			updateTargetDirection();
			nextPosition = navigation.getNavigationPosition();
			makeStep();
		} else if (selfCategory == SelfCategory.WAIT) {
			// do nothing
		} else if (selfCategory == SelfCategory.EVADE) {
			INavigation evasionNavigation = new NavigationEvasion();
			evasionNavigation.initialize(this, topography, null);
			nextPosition = evasionNavigation.getNavigationPosition();
			makeStep();
		} else {
			throw new IllegalArgumentException("Unsupported SelfCategory: " + selfCategory);
		}

		FootStep currentFootstep = new FootStep(position, getPosition(), startTimeStep, endTimeStep);
		getTrajectory().add(currentFootstep);
		getFootstepHistory().add(currentFootstep);
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

	public VPoint stepAwayFromCollision(final Pedestrian collisionPed) {
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

	public VPoint computeTargetStep() {
		return UtilsBHM.getTargetStep(this, getPosition(), getTargetDirection());
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
			Target target = topography.getTarget(getNextTargetId());
			if (!target.getShape().contains(getPosition())) {

				targetDirection = targetDirectionStrategy.getTargetDirection(target);

				for (DirectionAddend da : directionAddends) {
					targetDirection = targetDirection.add(da.getDirectionAddend(targetDirection));
				}

				//TODO: if this happens it might cause problems dependent on the heuristics choose.
				if(targetDirection.distanceToOrigin() < GeometryUtils.DOUBLE_EPS) {
					targetDirection = VPoint.ZERO;
				}
				else {
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

	public VPoint computeMovementProjection() {
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
	public boolean collidesWithPedestrianOnPath(VPoint position) {
		boolean isCollision = false;

		Pedestrian collision = findCollisionPedestrian(position, true);

		if (collision != null && TopographyHelper.otherPedestrianIsCloserToTarget(topography,this, collision)) {
			isCollision = true;
		}

		return isCollision;
	}

	/**
	 * If findAny is false, return the first collision on the path to position.
	 * If findAny is true, return the first collision that was found (could be any).
	 */
	public Pedestrian findCollisionPedestrian(VPoint position, boolean findAny) {

		Pedestrian result = null;
		double minDistance = Double.MAX_VALUE;

		VLine stepLine = new VLine(getPosition(), position);
		double len = stepLine.length();
		VPoint midPoint = stepLine.midPoint();

		for (Pedestrian other : topography.getSpatialMap(Pedestrian.class)
				.getObjects(midPoint, len *0.5 + 2 * getRadius() + attributesBHM.getSpaceToKeep())) {
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
		if (detectObstacleProximity(position, getRadius()).isEmpty()) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * This does not check collisions on the path, just collisions with position!
	 */
	public List<Obstacle> detectObstacleProximity(@NotNull VPoint position, double proximity) {

		Collection<Obstacle> obstacles = topography.getObstacles();
		List<Obstacle> result = new LinkedList<>();

		for (Obstacle obstacle : obstacles) {
 			if (obstacle.getShape().distance(position) < proximity) {
				result.add(obstacle);
			}
		}

		return result;
	}

	Optional<Obstacle> detectClosestObstacleProximity(@NotNull final VPoint position, double proximity) {

		Collection<Obstacle> obstacles = topography.getObstacles();
		Obstacle obs = null;
		double minDistance = Double.MAX_VALUE;

		for (Obstacle obstacle : obstacles) {
			double distance = obstacle.getShape().distance(position);
			if (distance < proximity && distance < minDistance) {
				obs = obstacle;
				minDistance = distance;
			}
		}
		return Optional.ofNullable(obs);
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

	/*
	Benedikt Zoennchen: These methods are my attempt to use the (negative) gradient of the traveling time for computing the target direction which
	does not work reliable at the moment. The (negative) gradient might point inside an obstacle!

	private VPoint computeTargetDirectionByStepGradient() {
		double distance = topography.getTarget(getNextTargetId()).getShape().distance(getPosition());
		if(distance > 0 && distance < getStepLength()) {
			return topography.getTarget(getNextTargetId()).getShape().closestPoint(getPosition()).setMagnitude(getStepLength());
		}

		VPoint bestArg = getPosition();
		double bestVal = potentialFieldTarget.getPotential(bestArg, this);

		double h = 0.01;
		VPoint nextPosition = getPosition();
		double stepLenSq = getStepLength() * getStepLength();

		while (Math.abs(nextPosition.distanceSq(getPosition()) - stepLenSq) > h) {
			VPoint gradient = potentialFieldTarget.getTargetPotentialGradient(nextPosition, this).multiply(-1.0);
			nextPosition = nextPosition.add(gradient.scalarMultiply(h));
			double val = potentialFieldTarget.getPotential(nextPosition, this);
			if(val < bestVal) {
				bestVal = val;
			} else {
				break;
			}

		}

		return nextPosition.subtract(getPosition()).norm();
	}

	private VPoint computeTargetDirectionByLeap() {
		double distance = topography.getTarget(getNextTargetId()).getShape().distance(getPosition());
		if(distance > 0 && distance < getStepLength()) {
			return topography.getTarget(getNextTargetId()).getShape().closestPoint(getPosition()).setMagnitude(getStepLength());
		} else {
			VPoint gradient1 = computeAdaptedGradient(computeTargetDirectionByGradient());
			VPoint gradient2 = computeAdaptedGradient(potentialFieldTarget.getTargetPotentialGradient(getPosition().add(gradient1.setMagnitude(getStepLength())), this).multiply(-1.0));
			return gradient1.add(gradient2).norm();
		}
	}

	private VPoint computeAdaptedGradient(@NotNull final VPoint gradient) {
		VPoint newGradient = gradient;

		// agent may walked inside an obstacle
		if(gradient.distanceSq(new VPoint(0,0)) < GeometryUtils.DOUBLE_EPS) {
			Optional<Obstacle> obstacle = detectClosestObstacleProximity(getPosition(), getRadius());
			if(obstacle.isPresent()) {
				VPoint closestPoint = obstacle.get().getShape().closestPoint(getPosition());

				VPoint direction = getPosition().subtract(closestPoint);
				newGradient = direction.setMagnitude(direction.distanceToOrigin() + getRadius());
			}
		}

		return newGradient;
	}
	*/
}

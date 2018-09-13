package org.vadere.simulator.models.osm;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.simulator.models.SpeedAdjuster;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizer;
import org.vadere.simulator.models.osm.stairOptimization.StairStepOptimizer;
import org.vadere.simulator.models.osm.updateScheme.UpdateSchemeEventDriven;
import org.vadere.simulator.models.osm.updateScheme.UpdateSchemeOSM;
import org.vadere.simulator.models.osm.updateScheme.UpdateSchemeOSM.CallMethod;
import org.vadere.simulator.models.osm.updateScheme.UpdateSchemeParallel;
import org.vadere.simulator.models.osm.updateScheme.UpdateSchemeSequential;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.models.potential.fields.PotentialFieldTargetRingExperiment;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Stairs;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.UpdateType;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class PedestrianOSM extends Pedestrian {

	private final AttributesOSM attributesOSM;
	private final transient StepCircleOptimizer stepCircleOptimizer;
	private final transient UpdateSchemeOSM updateScheme;
	private final transient Topography topography;
	private final double stepLength;
	private final double stepDeviation;
	private final double minStepLength;
	private transient IPotentialFieldTarget potentialFieldTarget;
	private transient PotentialFieldObstacle potentialFieldObstacle;
	private transient PotentialFieldAgent potentialFieldPedestrian;
	private transient List<SpeedAdjuster> speedAdjusters;
	private double durationNextStep;
	private VPoint nextPosition;
	private VPoint lastPosition;

	// for unit time clock update...
	private double timeCredit;
	// for event driven update...
	private double timeOfNextStep;

	private transient Collection<? extends Agent> relevantPedestrians;

	// calculated by (current position - last position)/(period of time).
	private double speedByAbsoluteDistance;

	private LinkedList<Pair<Double, Double>> strides; // left = length, right = time
	private StairStepOptimizer stairStepOptimizer;

	@SuppressWarnings("unchecked")
	PedestrianOSM(AttributesOSM attributesOSM,
				  AttributesAgent attributesPedestrian, Topography topography,
				  Random random, IPotentialFieldTarget potentialFieldTarget,
				  PotentialFieldObstacle potentialFieldObstacle,
				  PotentialFieldAgent potentialFieldPedestrian,
				  List<SpeedAdjuster> speedAdjusters,
				  StepCircleOptimizer stepCircleOptimizer) {

		super(attributesPedestrian, random);

		this.attributesOSM = attributesOSM;
		this.topography = topography;
		this.potentialFieldTarget = potentialFieldTarget;
		this.potentialFieldObstacle = potentialFieldObstacle;
		this.potentialFieldPedestrian = potentialFieldPedestrian;
		this.stepCircleOptimizer = stepCircleOptimizer;
		this.updateScheme = createUpdateScheme(attributesOSM.getUpdateType(), this);

		this.speedAdjusters = speedAdjusters;
		this.relevantPedestrians = new HashSet<>();
		this.timeCredit = 0;

		this.setVelocity(new Vector2D(0, 0));

		this.stepDeviation = random.nextGaussian() * attributesOSM.getStepLengthSD();

		this.stepLength = attributesOSM.getStepLengthIntercept() + this.stepDeviation
				+ attributesOSM.getStepLengthSlopeSpeed() * getFreeFlowSpeed();
		if (attributesOSM.isMinimumStepLength()) {
			this.minStepLength = attributesOSM.getStepLengthIntercept();
		} else {
			this.minStepLength = 0;
		}

		this.strides = new LinkedList<>();
	}

	private static UpdateSchemeOSM createUpdateScheme(UpdateType updateType, PedestrianOSM pedestrian) {

		UpdateSchemeOSM result;

		switch (updateType) {
			case EVENT_DRIVEN:
				result = new UpdateSchemeEventDriven(pedestrian, pedestrian.topography);
				break;
			case PARALLEL:
				result = new UpdateSchemeParallel(pedestrian);
				break;
			case SEQUENTIAL:
				result = new UpdateSchemeSequential(pedestrian, pedestrian.topography);
				break;
			default:
				result = new UpdateSchemeSequential(pedestrian, pedestrian.topography);
		}

		return result;
	}

	public void update(double timeStepInSec, double currentTimeInSec, CallMethod callMethod) {
		double lastSimTimeInSec = currentTimeInSec - timeStepInSec;

		// clear the old strides to avoid large linked lists
		if(!strides.isEmpty() && lastSimTimeInSec < strides.peekFirst().getRight()) {
			clearStrides();
		}

		this.updateScheme.update(timeStepInSec, currentTimeInSec, callMethod);
	}

	public void updateNextPosition() {

		if (PotentialFieldTargetRingExperiment.class.equals(potentialFieldTarget.getClass())) {
			VCircle reachableArea = new VCircle(getPosition(), getStepSize());
			this.relevantPedestrians = potentialFieldPedestrian
					.getRelevantAgents(reachableArea, this, topography);

			nextPosition = stepCircleOptimizer.getNextPosition(this, reachableArea);
			// if (nextPosition.distance(this.getPosition()) < this.minStepLength) {
			// nextPosition = this.getPosition();
			// }
		} else if (!hasNextTarget()) {
			this.nextPosition = getPosition();
		} else if (topography.getTarget(getNextTargetId()).getShape().contains(getPosition())) {
			this.nextPosition = getPosition();
		} else {
			VCircle reachableArea = new VCircle(getPosition(), getStepSize());

			this.relevantPedestrians = potentialFieldPedestrian
					.getRelevantAgents(reachableArea, this, topography);


			// get stairs pedestrian is on - remains null if on area
			Stairs stairs = null;
			for (Stairs singleStairs : topography.getStairs()) {
				if (singleStairs.getShape().contains(getPosition())) {
					stairs = singleStairs;
					break;
				}
			}

			if (stairs == null) { // meaning pedestrian is on area
				nextPosition = stepCircleOptimizer.getNextPosition(this, reachableArea);
			} else {
				stairStepOptimizer = new StairStepOptimizer(stairs);
				reachableArea = new VCircle(getPosition(), stairs.getTreadDepth() * 1.99);
				nextPosition = stairStepOptimizer.getNextPosition(this, reachableArea);
				// Logger.getLogger(this.getClass()).info("Pedestrian " + this.getId() + " is on
				// stairs @position: " + nextPosition);
			}
		}

	}

	public void makeStep(double stepTime) {
		VPoint currentPosition = getPosition();

		if (nextPosition.equals(currentPosition)) {
			timeCredit = 0;
			setVelocity(new Vector2D(0, 0));

		} else {
			timeCredit = timeCredit - durationNextStep;
			setPosition(nextPosition);

			// compute velocity by forward difference
			setVelocity(new Vector2D(nextPosition.x - currentPosition.x,
					nextPosition.y - currentPosition.y).multiply(1.0 / stepTime));
		}

		strides.add(Pair.of(currentPosition.distance(nextPosition), getTimeOfNextStep()));
	}

	public double getStepSize() {
		if (attributesOSM.isDynamicStepLength()) {
			double step = attributesOSM.getStepLengthIntercept()
					+ attributesOSM.getStepLengthSlopeSpeed()
					* getDesiredSpeed()
					+ stepDeviation;
			return step;
		} else {
			return stepLength;
		}
	}

	public double getDesiredSpeed() {
		double desiredSpeed = getFreeFlowSpeed();

		for (SpeedAdjuster adjuster : speedAdjusters) {
			desiredSpeed = adjuster.getAdjustedSpeed(this, desiredSpeed);
		}

		return desiredSpeed;
	}

	public double getPotential(VPoint newPos) {

		double targetPotential = potentialFieldTarget.getPotential(newPos, this);

		double pedestrianPotential = potentialFieldPedestrian
				.getAgentPotential(newPos, this, relevantPedestrians);
		double obstacleRepulsionPotential = potentialFieldObstacle
				.getObstaclePotential(newPos, this);
		return targetPotential + pedestrianPotential
				+ obstacleRepulsionPotential;
	}

	public void clearStrides() {
		strides.clear();
	}

	// Getters...

	public double getTargetPotential(VPoint pos) {
		return potentialFieldTarget.getPotential(pos, this);
	}

	public IPotentialFieldTarget getPotentialFieldTarget() {
		return potentialFieldTarget;
	}

	public Vector2D getTargetGradient(VPoint pos) {
		return potentialFieldTarget.getTargetPotentialGradient(pos, this);
	}

	public Vector2D getObstacleGradient(VPoint pos) {
		return potentialFieldObstacle.getObstaclePotentialGradient(pos, this);
	}

	public Vector2D getPedestrianGradient(VPoint pos) {
		return potentialFieldPedestrian.getAgentPotentialGradient(pos,
				new Vector2D(0, 0), this, relevantPedestrians);
	}

	public double getTimeOfNextStep() {
		return timeOfNextStep;
	}

	public void setTimeOfNextStep(double timeOfNextStep) {
		this.timeOfNextStep = timeOfNextStep;
	}

	public VPoint getNextPosition() {
		return nextPosition;
	}

	public void setNextPosition(VPoint nextPosition) {
		this.nextPosition = nextPosition;
	}

	public VPoint getLastPosition() {
		return lastPosition;
	}

	public void setLastPosition(VPoint lastPosition) {
		this.lastPosition = lastPosition;
	}

	public double getTimeCredit() {
		return timeCredit;
	}

	public void setTimeCredit(double timeCredit) {
		this.timeCredit = timeCredit;
	}


	// Setters...

	public Collection<? extends Agent> getRelevantPedestrians() {
		return relevantPedestrians;
	}

	public double getDurationNextStep() {
		return durationNextStep;
	}

	public void setDurationNextStep(double durationNextStep) {
		this.durationNextStep = durationNextStep;
	}

	public AttributesOSM getAttributesOSM() {
		return attributesOSM;
	}

	public LinkedList<Pair<Double, Double>> getStrides() {
		return strides;
	}

	public Topography getTopography() {
		return topography;
	}

	public double getMinStepLength() {
		return minStepLength;
	}

	@Override
	public PedestrianOSM clone() {
		throw new RuntimeException("clone is not supported for PedestrianOSM; it seems hard to implement.");
	}

}

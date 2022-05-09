package org.vadere.simulator.models.osm;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.StepSizeAdjuster;
import org.vadere.simulator.models.osm.optimization.OptimizationMetric;
import org.vadere.state.attributes.models.AttributesCombinedPotentialStrategy;
import org.vadere.simulator.models.potential.combinedPotentials.*;
import org.vadere.state.attributes.models.AttributesPedestrianRepulsionPotentialStrategy;
import org.vadere.util.geometry.shapes.*;
import org.vadere.simulator.models.SpeedAdjuster;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizer;
import org.vadere.simulator.models.osm.stairOptimization.StairStepOptimizer;
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
;

import java.util.*;
import java.util.stream.Collectors;

public class PedestrianOSM extends Pedestrian {

	private final Random random;

	private final AttributesOSM attributesOSM;
	private final transient StepCircleOptimizer stepCircleOptimizer;
	private final transient Topography topography;
	private final double stepLength;
	private final double stepDeviation;
	private final double minStepLength;

	private transient IPotentialFieldTarget potentialFieldTarget;
	private transient PotentialFieldObstacle potentialFieldObstacle;
	private AttributesCombinedPotentialStrategy attributesCombinedPotentialStrategy;

	public PotentialFieldAgent getPotentialFieldPedestrian() {
		return potentialFieldPedestrian;
	}

	private transient PotentialFieldAgent potentialFieldPedestrian;
	// A setter is provided to be able to change strategy at runtime (e.g. by stimuli).
	private transient ICombinedPotentialStrategy combinedPotentialStrategy;
	private transient List<SpeedAdjuster> speedAdjusters;
	private transient List<StepSizeAdjuster> stepSizeAdjusters;
	private VPoint nextPosition;
	private VPoint lastPosition;

	// for event driven update...
	private double timeOfNextStep;

	private transient Collection<? extends Agent> relevantPedestrians;

	// calculated by (current position - last position)/(period of time).
	private double speedByAbsoluteDistance;

	private LinkedList<Pair<Double, Double>> strides; // left = length, right = time
	private StairStepOptimizer stairStepOptimizer;

	@SuppressWarnings("unchecked")
	public PedestrianOSM(AttributesOSM attributesOSM,
				  AttributesAgent attributesPedestrian, Topography topography,
				  Random random, IPotentialFieldTarget potentialFieldTarget,
				  PotentialFieldObstacle potentialFieldObstacle,
				  PotentialFieldAgent potentialFieldPedestrian,
				  List<SpeedAdjuster> speedAdjusters,
				  StepCircleOptimizer stepCircleOptimizer) {

		super(attributesPedestrian, random);

		this.random = random;

		this.attributesOSM = attributesOSM;
		this.topography = topography;
		this.potentialFieldTarget = potentialFieldTarget;
		this.potentialFieldObstacle = potentialFieldObstacle;
		this.potentialFieldPedestrian = potentialFieldPedestrian;
		this.combinedPotentialStrategy = new TargetAttractionStrategy(potentialFieldTarget, potentialFieldObstacle, potentialFieldPedestrian);
		this.stepCircleOptimizer = stepCircleOptimizer;

		this.speedAdjusters = speedAdjusters;
		this.stepSizeAdjusters = new LinkedList<>();
		this.relevantPedestrians = new HashSet<>();
		this.timeOfNextStep = INVALID_NEXT_EVENT_TIME;

		this.setVelocity(new Vector2D(0, 0));

		this.stepDeviation = random.nextGaussian() * attributesOSM.getStepLengthSD();

		this.stepLength = attributesOSM.getStepLengthIntercept() + this.stepDeviation
				+ attributesOSM.getStepLengthSlopeSpeed() * getFreeFlowSpeed();
		if (attributesOSM.isMinimumStepLength()) {
			this.minStepLength = attributesOSM.getMinStepLength();
		} else {
			this.minStepLength = 0;
		}

		this.lastPosition = getPosition();
		this.nextPosition = getPosition();
		this.strides = new LinkedList<>();
	}

	private PedestrianOSM(@NotNull final PedestrianOSM other) {
		super(other);

		this.attributesOSM = other.attributesOSM;
		this.topography = other.topography;
		this.potentialFieldTarget = other.potentialFieldTarget;
		this.potentialFieldObstacle = other.potentialFieldObstacle;
		this.potentialFieldPedestrian = other.potentialFieldPedestrian;
		this.combinedPotentialStrategy = other.combinedPotentialStrategy;
		this.stepCircleOptimizer = other.stepCircleOptimizer;

		this.speedAdjusters = new LinkedList<>(other.speedAdjusters);
		this.stepSizeAdjusters = new LinkedList<>(other.stepSizeAdjusters);
		this.relevantPedestrians = new ArrayList<>(other.relevantPedestrians);
		this.timeOfNextStep = INVALID_NEXT_EVENT_TIME;
		this.stepDeviation = other.stepDeviation;
		this.stepLength = other.stepLength;
		this.minStepLength = other.minStepLength;
		this.lastPosition = other.lastPosition;
		this.nextPosition = other.nextPosition;
		this.strides = new LinkedList<>(other.strides);
		this.random = other.random;
	}

	/*public void update(double timeStepInSec, double currentTimeInSec, CallMethod callMethod) {
		double lastSimTimeInSec = currentTimeInSec - timeStepInSec;

		// clear the old strides to avoid large linked lists
		if(!strides.isEmpty() && lastSimTimeInSec < strides.peekFirst().getRight()) {
			clearStrides();
		}

		this.updateScheme.update(timeStepInSec, currentTimeInSec, callMethod);
	}*/

	/**
	 * Expensive call!
	 */
	public void updateNextPosition() {

		if (PotentialFieldTargetRingExperiment.class.equals(potentialFieldTarget.getClass())) {
			VCircle reachableArea = new VCircle(getPosition(), getFreeFlowStepSize());

			refreshRelevantPedestrians();
			nextPosition = stepCircleOptimizer.getNextPosition(this, reachableArea);

			// if (nextPosition.distance(this.getPosition()) < this.minStepLength) {
			// nextPosition = this.getPosition();
			// }
		} else if (!hasNextTarget() || getDurationNextStep() > getAttributesOSM().getMaxStepDuration()) {
			this.nextPosition = getPosition();
		} else if (isCurrentTargetAnAgent() == false && topography.getTarget(getNextTargetId()).getShape().contains(getPosition())) {
			this.nextPosition = getPosition();
		} else {
			VCircle reachableArea = new VCircle(getPosition(), getDesiredStepSize());

			// get stairs object an agent may be on - remains null if agent is on area
			Stairs stairs = null;
			for (Stairs singleStairs : topography.getStairs()) {
				if (singleStairs.getShape().contains(getPosition())) {
					stairs = singleStairs;
					break;
				}
			}

			if (stairs == null) { // --> agent is on area

				refreshRelevantPedestrians();
				nextPosition = stepCircleOptimizer.getNextPosition(this, reachableArea);

				if(attributesOSM.isMinimumStepLength() && getPosition().distance(nextPosition) < minStepLength) {
					nextPosition = getPosition();
				}
				/*else if(potentialFieldTarget.getPotential(nextPosition, this) >= potentialFieldTarget.getPotential(getPosition(), this)) {
					nextPosition = getPosition();
				}*/

			} else {
				stairStepOptimizer = new StairStepOptimizer(stairs);
				reachableArea = new VCircle(getPosition(), stairs.getTreadDepth() * 1.99);

				refreshRelevantPedestrians();
				nextPosition = stairStepOptimizer.getNextPosition(this, reachableArea);
				// Logger.getLogger(this.getClass()).info("Pedestrian " + this.getId() + " is on
				// stairs @position: " + nextPosition);
			}
		}

	}

	public void updateNextPosition(VShape reachableArea) {

		if (!hasNextTarget() || getDurationNextStep() > getAttributesOSM().getMaxStepDuration()) {
			this.nextPosition = getPosition();
		} else if (isCurrentTargetAnAgent() == false && topography.getTarget(getNextTargetId()).getShape().contains(getPosition())) {
			this.nextPosition = getPosition();
		} else {
			// get stairs object an agent may be on - remains null if agent is on area
			Stairs stairs = null;
			for (Stairs singleStairs : topography.getStairs()) {
				if (singleStairs.getShape().contains(getPosition())) {
					stairs = singleStairs;
					break;
				}
			}

			if (stairs == null) { // --> agent is on area
				refreshRelevantPedestrians();
				nextPosition = stepCircleOptimizer.getNextPosition(this, reachableArea);

				if(attributesOSM.isMinimumStepLength() && getPosition().distance(nextPosition) < minStepLength) {
					nextPosition = getPosition();
				}
			} else {
				stairStepOptimizer = new StairStepOptimizer(stairs);
				reachableArea = new VCircle(getPosition(), stairs.getTreadDepth() * 1.99);

				refreshRelevantPedestrians();
				nextPosition = stairStepOptimizer.getNextPosition(this, reachableArea);
			}
		}

	}

	/**
	 * Returns the constant free flow step size
	 *
	 * @return the free flow step size
	 */
	public double getFreeFlowStepSize() {
		/*if (attributesOSM.isDynamicStepLength()) {
			double step = attributesOSM.getStepLengthIntercept()
					+ attributesOSM.getStepLengthSlopeSpeed()
					* getDesiredSpeed()
					+ stepDeviation;
			return step;
		} else {*/
			return stepLength;
		//}
	}

	/**
	 * Returns the actual step size of the last step.
	 *
	 * @return the actual step size of the last step
	 */
	private double getStepSize() {
		if(nextPosition != null) {
			return nextPosition.distance(getPosition());
		}
		return 0;
	}

	/**
	 * Returns the step size the agent is currently trying to achieve. This step
	 * size is dynamic i.e. it might be influenced by the situation via a dynamic
	 * desired speed influenced by {@link SpeedAdjuster}.
	 * If this step size is larger or smaller than {@link #getFreeFlowStepSize()} the agent
	 * tries to accelerate or decelerate respectively.
	 *
	 * @return the currently desired step size which depends on the dynamic of the simulation
	 */
	public double getDesiredStepSize() {
		double desiredStepSize = getFreeFlowStepSize();
		if (attributesOSM.isDynamicStepLength()) {
			double step = attributesOSM.getStepLengthIntercept()
					+ attributesOSM.getStepLengthSlopeSpeed()
					* getDesiredSpeed()
					+ stepDeviation;
			return step;
		}
		return desiredStepSize;
	}

	/**
	 * Returns the desired speed an agent is currently trying to achieve. This speed
	 * is dynamic i.e. it might be influenced by the situation via a dynamic
	 * desired speed influenced by {@link SpeedAdjuster}.
	 *
	 * @return the desired speed an agent is currently trying to achieve
	 */
	public double getDesiredSpeed() {
		double desiredSpeed = getFreeFlowSpeed();
		for (SpeedAdjuster adjuster : speedAdjusters) {
			desiredSpeed = adjuster.getAdjustedSpeed(this, desiredSpeed);
		}

		return desiredSpeed;
	}

	public double getPotential(IPoint newPos) {
		return combinedPotentialStrategy.getValue(newPos, this, relevantPedestrians);
	}

	public void clearStrides() {
		strides.clear();
	}

	// TODO: Group getters and setters correctly.

	// Getters

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

	public ICombinedPotentialStrategy getCombinedPotentialStrategy() {
		return combinedPotentialStrategy;
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

	public void refreshRelevantPedestrians() {
		VCircle reachableArea = new VCircle(getPosition(), getFreeFlowStepSize());
		setRelevantPedestrians(potentialFieldPedestrian.getRelevantAgents(reachableArea, this, getTopography()));
	}


	// Setters...

	public void setRelevantPedestrians(@NotNull final Collection<? extends Agent> relevantPedestrians) {
		this.relevantPedestrians = relevantPedestrians;
	}

	public void setCombinedPotentialStrategy(CombinedPotentialStrategy newStrategy) {
		if (newStrategy == CombinedPotentialStrategy.TARGET_ATTRACTION_STRATEGY) {
			this.combinedPotentialStrategy = new TargetAttractionStrategy(this.potentialFieldTarget,
						this.potentialFieldObstacle,
						this.potentialFieldPedestrian);
		} else if (newStrategy == CombinedPotentialStrategy.TARGET_REPULSION_STRATEGY) {
			this.combinedPotentialStrategy = new TargetRepulsionStrategy(this.potentialFieldTarget,
					this.potentialFieldObstacle,
					this.potentialFieldPedestrian);
		} else if (newStrategy == CombinedPotentialStrategy.PEDESTRIAN_REPULSION_STRATEGY){
			this.combinedPotentialStrategy = new PedestrianRepulsionStrategy(this.potentialFieldTarget,
					this.potentialFieldObstacle,
					this.potentialFieldPedestrian,
					(AttributesPedestrianRepulsionPotentialStrategy) this.attributesCombinedPotentialStrategy);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	public Collection<? extends Agent> getRelevantPedestrians() {
		return relevantPedestrians;
	}

	/**
	 * Returns a constant step duration defined by the free flow step size and the free flow velocity.
	 *
	 * @return the step duration of this agent
	 */
	public double getDurationNextStep() {
		return Math.min(getAttributesOSM().getMaxStepDuration(), getDesiredStepSize() / getDesiredSpeed());
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

	public ArrayList<OptimizationMetric> getOptimizationMetricElements(){
		// Function that can be called by a processor to obtain metric data (PedestrianMetricOptimizationProcessor).
		var values = this.stepCircleOptimizer.getCurrentMetricValues();
		this.stepCircleOptimizer.clearMetricValues();
		return values;
	}

	@Override
	public PedestrianOSM clone() {
		return new PedestrianOSM(this);
	}

	@Override
	public String toString() {
		return "id = " + getId() + " memory " + super.toString();
	}

	public LinkedList<Pedestrian> getPedGroupMembers(){

		LinkedList<Pedestrian> peds = new LinkedList<>();
		
		if (this.getGroupIds() != null) {
			if (this.getGroupIds().size() == 1) {
				for (int i : getGroupIds()) {
					Collection<Pedestrian> pp = getTopography().getPedestrianDynamicElements().getElements().stream().filter(p -> p.getGroupIds().getFirst() == i && p.getId() != getId()).collect(Collectors.toList());

					for (Pedestrian ped : pp) {
						peds.add(ped);
					}
				}
			}
		}
		return peds;
	}

	public void setCombinedPotentialStrategyAttributes(AttributesCombinedPotentialStrategy attributes) {
		this.attributesCombinedPotentialStrategy = attributes;
	}

	public AttributesCombinedPotentialStrategy getAttributesCombinedPotentialStrategy(){
		return this.attributesCombinedPotentialStrategy;
	}
}

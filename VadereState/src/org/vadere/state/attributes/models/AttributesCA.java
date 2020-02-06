package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.types.MovementType;
import org.vadere.state.types.OptimizationType;
import org.vadere.state.types.UpdateType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class contains a default parameter setting for emulating the cellular automaton with the Optimal Steps Model
 *
 * @author hm-mgoedel
 */

@ModelAttributeClass
public class AttributesCA extends AttributesOSM {

	/** Von-Neumann neighbourhood is realized with stepCircleResolution = 4 and numberOfCircles = 1
	 * One advantage of von-Neumann neighbourhood compared to Moore neighbourhood is that the step size to the next cell
	 * is the same for all neighbors with von-Neumann neighbourhood.
	 */
	private int stepCircleResolution = 4;
	private int numberOfCircles = 1;

	// Discrete is used so that purely the defined positions will be used (enforce grid)
	private OptimizationType optimizationType = OptimizationType.DISCRETE;

	// enforce a static grid
	private boolean varyStepDirection = false;

	private MovementType movementType = MovementType.ARBITRARY;


	// this should be equivalent to the cell size that is used with the CA
	// todo: fix this to the pedestrian's radius?
	private double stepLengthIntercept = 0.4;

	private double stepLengthSlopeSpeed = 0.0;

	// standard deviation = 0 to enforce the same step length for each agent (enforce grid)
	private double stepLengthSD = 0.0;

	private double movementThreshold = 0;


	// step length can only be zero or one grid cell, a minimum step length is not meaningful in CA
	private double minStepLength = 0.0;
	private boolean minimumStepLength = false;
	private boolean dynamicStepLength = false;


	private double maxStepDuration = Double.MAX_VALUE;



	private UpdateType updateType = UpdateType.PARALLEL;

	/**
	 * If <tt>true</tt> this avoids agent jumping over small walls. However, this does not fix the problem that
	 * the target potential computation fails due to small obstacles. Since this is a quick fix and the
	 * test is very expensive the default should be false!
	 */
	private boolean seeSmallWalls = false;


	private String targetPotentialModel = "org.vadere.simulator.models.potential.fields.PotentialFieldTargetGrid";
	private String pedestrianPotentialModel = "org.vadere.simulator.models.potential.PotentialFieldPedestrianCA";
	private String obstaclePotentialModel = "org.vadere.simulator.models.potential.PotentialFieldObstacleCA";
	private List<String> submodels = new LinkedList<>();

	public AttributesCA() {}

	// Getters...
	public int getStepCircleResolution() {
		return stepCircleResolution;
	}

	public int getNumberOfCircles() {
		return numberOfCircles;
	}

	public double getMaxStepDuration() {
		return maxStepDuration;
	}

	public boolean isVaryStepDirection() {
		return varyStepDirection;
	}

	public double getStepLengthIntercept() {
		return stepLengthIntercept;
	}

	public double getStepLengthSlopeSpeed() {
		return stepLengthSlopeSpeed;
	}

	public double getStepLengthSD() {
		return stepLengthSD;
	}

	public double getMovementThreshold() {
		return movementThreshold;
	}

	public OptimizationType getOptimizationType() {
		return optimizationType;
	}

	public boolean isDynamicStepLength() {
		return dynamicStepLength;
	}

	public UpdateType getUpdateType() {
		return updateType;
	}

	public MovementType getMovementType() {
		return movementType;
	}

	public boolean isSeeSmallWalls() {
		return seeSmallWalls;
	}

	public boolean isMinimumStepLength() {
		return minimumStepLength;
	}

	public String getTargetPotentialModel() {
		return targetPotentialModel;
	}

	public String getPedestrianPotentialModel() {
		return pedestrianPotentialModel;
	}

	public String getObstaclePotentialModel() {
		return obstaclePotentialModel;
	}

	/**
	 * Return a copy of the submodel class names.
	 *
	 * @return a copy of the submodel class names
	 */
	public List<String> getSubmodels() {
		return new ArrayList<>(submodels);
	}

	public double getMinStepLength() {
		return minStepLength;
	}
}

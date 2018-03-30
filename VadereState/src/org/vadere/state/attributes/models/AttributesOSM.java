package org.vadere.state.attributes.models;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.types.MovementType;
import org.vadere.state.types.OptimizationType;
import org.vadere.state.types.UpdateType;

public class AttributesOSM extends Attributes {

	private int stepCircleResolution = 18;
	private int numberOfCircles = 1;
	private boolean varyStepDirection = false;
	private double stepLengthIntercept = 0.4625;
	private double stepLengthSlopeSpeed = 0.2345;
	private double stepLengthSD = 0.036;
	private double movementThreshold = 0;
	private OptimizationType optimizationType = OptimizationType.DISCRETE;
	private MovementType movementType = MovementType.ARBITRARY;
	private boolean dynamicStepLength = false;
	private UpdateType updateType = UpdateType.EVENT_DRIVEN;
	private boolean seeSmallWalls = false;
	private boolean minimumStepLength = false;
	private String targetPotentialModel = "org.vadere.simulator.models.potential.fields.PotentialFieldTargetGrid";
	private String pedestrianPotentialModel = "org.vadere.simulator.models.potential.PotentialFieldPedestrianCompactSoftshell";
	private String obstaclePotentialModel = "org.vadere.simulator.models.potential.PotentialFieldObstacleCompactSoftshell";
	private List<String> submodels = new LinkedList<>();

	public AttributesOSM() {}

	// Getters...
	public int getStepCircleResolution() {
		return stepCircleResolution;
	}

	public int getNumberOfCircles() {
		return numberOfCircles;
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

	/** Return a copy of the submodel class names. */
	public List<String> getSubmodels() {
		return new ArrayList<>(submodels);
	}



}

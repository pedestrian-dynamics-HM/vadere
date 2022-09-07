package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.LinkedList;

@ModelAttributeClass
public class AttributesBHM extends Attributes {

    /**
     * My documentation for xyz.
     * @author BK
     * @see AttributesBMM
     */
	private final double stepLengthIntercept = 0.4625;
	private final double stepLengthSlopeSpeed = 0.2345;
	private final double stepLengthSD = 0.036;
	private final boolean stepLengthDeviation = false;

	// TODO: Remove boolean navigation variables (including migration). Instead, use new "NavigationBuilder".
	private final boolean navigationCluster = false;
	private final boolean navigationFollower = false;
	private final boolean followerProximityNavigation = true;
	private final String navigationModel = "NavigationProximity";

	private final boolean directionWallDistance = false;
	private final boolean tangentialEvasion = true;
	private final boolean sidewaysEvasion = false;
	private final boolean onlyEvadeContraFlow = false;
	private final boolean makeSmallSteps = false;

	private final boolean differentBehaviour = false;
	private final LinkedList<Double> differentEvasionBehaviourPercentage = new LinkedList<>();
	private final boolean varyingBehaviour = false;
	private final boolean adaptiveBehaviourDensity = false;
	private final LinkedList<Integer> adaptiveBehaviourStepsRemained = new LinkedList<>();
	private final boolean switchBehaviour = false;

	private final double evasionDetourThreshold = 0.1;
	private final double onlyEvadeContraFlowAngle = Math.PI * 2.0 / 3.0;
	private final double followerAngleMovement = Math.PI / 2.0;
	private final double followerAnglePosition = Math.PI / 2.0;
	private final double followerDistance = 10.0;
	private final int smallStepResolution = 5;
	private final int plannedStepsAhead = 5;
	private final double obstacleRepulsionReach = 1.0;
	private final double obstacleRepulsionMaxWeight = 0.5;
	private final double distanceToKeep = 0.5;
	private final double backwardsAngle = Math.PI / 2;

	private final boolean reconsiderOldTargets = false;
	private final double targetThresholdX = Double.MAX_VALUE;
	private final double targetThresholdY = Double.MAX_VALUE;

	private final double spaceToKeep = 0.01;
	private final boolean stepAwayFromCollisions = false;

	public double getStepLengthIntercept() {
		return stepLengthIntercept;
	}

	public double getStepLengthSlopeSpeed() {
		return stepLengthSlopeSpeed;
	}

	public double getStepLengthSD() {
		return stepLengthSD;
	}

	public boolean isStepLengthDeviation() {
		return stepLengthDeviation;
	}

	public boolean isNavigationCluster() {
		return navigationCluster;
	}

	public boolean isNavigationFollower() {
		return navigationFollower;
	}

	public boolean isFollowerProximityNavigation() {
		return followerProximityNavigation;
	}

	public String getNavigationModel() { return navigationModel; }

	public boolean isDirectionWallDistance() {
		return directionWallDistance;
	}

	public boolean isTangentialEvasion() {
		return tangentialEvasion;
	}

	public boolean isSidewaysEvasion() {
		return sidewaysEvasion;
	}

	public boolean isOnlyEvadeContraFlow() {
		return onlyEvadeContraFlow;
	}

	public boolean isMakeSmallSteps() {
		return makeSmallSteps;
	}


	public double getEvasionDetourThreshold() {
		return evasionDetourThreshold;
	}

	public double getOnlyEvadeContraFlowAngle() {
		return onlyEvadeContraFlowAngle;
	}

	public double getFollowerAngleMovement() {
		return followerAngleMovement;
	}

	public double getFollowerAnglePosition() {
		return followerAnglePosition;
	}

	public double getFollowerDistance() {
		return followerDistance;
	}

	public int getSmallStepResolution() {
		return smallStepResolution;
	}

	public int getPlannedStepsAhead() {
		return plannedStepsAhead;
	}

	public double getObstacleRepulsionReach() {
		return obstacleRepulsionReach;
	}

	public double getObstacleRepulsionMaxWeight() {
		return obstacleRepulsionMaxWeight;
	}

	public double getDistanceToKeep() {
		return distanceToKeep;
	}

	public double getBackwardsAngle() {
		return backwardsAngle;
	}

	public boolean isReconsiderOldTargets() {
		return reconsiderOldTargets;
	}

	public double getTargetThresholdX() {
		return targetThresholdX;
	}

	public double getTargetThresholdY() {
		return targetThresholdY;
	}

	public double getSpaceToKeep() {
		return spaceToKeep;
	}

	public boolean isStepAwayFromCollisions() {
		return stepAwayFromCollisions;
	}

	public boolean isDifferentBehaviour() {
		return differentBehaviour;
	}

	public LinkedList<Double> getDifferentEvasionBehaviourPercentage() {
		return differentEvasionBehaviourPercentage;
	}

	public boolean isVaryingBehaviour() {
		return varyingBehaviour;
	}

	public boolean isAdaptiveBehaviourDensity() {
		return adaptiveBehaviourDensity;
	}

	public LinkedList<Integer> getAdaptiveBehaviourStepsRemained() {
		return adaptiveBehaviourStepsRemained;
	}

	public boolean isSwitchBehaviour() {
		return switchBehaviour;
	}



}

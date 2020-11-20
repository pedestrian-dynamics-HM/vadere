package org.vadere.state.attributes.models;

import java.util.LinkedList;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

@ModelAttributeClass
public class AttributesBHM extends Attributes {

    /**
     * My documentation for xyz.
     * @author BK
     * @see AttributesBMM
     */
	private double stepLengthIntercept = 0.4625;
	private double stepLengthSlopeSpeed = 0.2345;
	private double stepLengthSD = 0.036;
	private boolean stepLengthDeviation = false;

	// TODO: Remove boolean navigation variables (including migration). Instead, use new "NavigationBuilder".
	private boolean navigationCluster = false;
	private boolean navigationFollower = false;
	private boolean followerProximityNavigation = true;
	private String navigationModel = "NavigationProximity";

	private boolean directionWallDistance = false;
	private boolean tangentialEvasion = true;
	private boolean sidewaysEvasion = false;
	private boolean onlyEvadeContraFlow = false;
	private boolean makeSmallSteps = false;

	private boolean differentBehaviour = false;
	private LinkedList<Double> differentEvasionBehaviourPercentage = new LinkedList<>();
	private boolean varyingBehaviour = false;
	private boolean adaptiveBehaviourDensity = false;
	private LinkedList<Integer> adaptiveBehaviourStepsRemained = new LinkedList<>();
	private boolean switchBehaviour = false;

	private double evasionDetourThreshold = 0.1;
	private double onlyEvadeContraFlowAngle = Math.PI * 2.0 / 3.0;
	private double followerAngleMovement = Math.PI / 2.0;
	private double followerAnglePosition = Math.PI / 2.0;
	private double followerDistance = 10.0;
	private int smallStepResolution = 5;
	private int plannedStepsAhead = 5;
	private double obstacleRepulsionReach = 1.0;
	private double obstacleRepulsionMaxWeight = 0.5;
	private double distanceToKeep = 0.5;
	private double backwardsAngle = Math.PI / 2;

	private boolean reconsiderOldTargets = false;
	private double targetThresholdX = Double.MAX_VALUE;
	private double targetThresholdY = Double.MAX_VALUE;

	private double spaceToKeep = 0.01;
	private boolean stepAwayFromCollisions = false;

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

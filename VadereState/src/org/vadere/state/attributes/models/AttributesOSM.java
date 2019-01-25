package org.vadere.state.attributes.models;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.types.MovementType;
import org.vadere.state.types.OptimizationType;
import org.vadere.state.types.UpdateType;

/**
 * <p>
 *     This class contains all parameters for the Optimal Steps Model with the exception of parameters for the three different
 *     potential functions (pedestrian-, target- and obstacle-potential) and without any submodel paraemters
 *     such as the Centroid Group Model {@link AttributesCGM}.
 * </p>
 *
 * <p>
 *     There exist different versions of the Optimal Steps Model which use different parameters such that not every parameter
 *     is used for every version and some parameters are only used if some other parameter has a specific value. The default version
 *     of the Optimal Steps Model is the one using the optimization on the disc and potential functions representing the personal
 *     spaces (see sivers-2016b). We deviate from sivers-2016b only in the concept of a minimal step length: In this implementation
 *     an agent will not move if its next position is closer than its minimal step length ({@link AttributesOSM#minStepLength}).
 * </p>
 *
 * <p>
 *     Parameters for the configuring the optimization method are:
 *     <ul>
 *         <li>{@link AttributesOSM#stepCircleResolution}</li>
 *         <li>{@link AttributesOSM#numberOfCircles}</li>
 *         <li>{@link AttributesOSM#optimizationType}</li>
 *         <li>{@link AttributesOSM#varyStepDirection}</li>
 *     </ul>
 *     Dependent on the used combination, they have different meanings!
 * </p>
 *
 * <p>
 *     The step length velocity correlation s(v) = a + b * v discussed in (seitz-2012) is realized via
 *     <ul>
 *         <li>{@link AttributesOSM#stepLengthIntercept}: a</li>
 *         <li>{@link AttributesOSM#stepLengthSlopeSpeed}: b</li>
 *     </ul>
 * </p>
 */
@ModelAttributeClass
public class AttributesOSM extends Attributes {

	/**
	 * <p>
	 *     Parameter of the optimization method: the number of points on the most outer circle.
	 *     These points will be used in different ways which depends on the {@link OptimizationType}.
	 *     <ul>
	 * 	       <li>OptimizationType.NELDER_MEAD (default): each neighbouring pair of points and the agent position is used as a starting simplex</li>
	 * 	       <li>OptimizationType.PSO: each point and the position of the agent is used as a starting position of a particle</li>
	 * 	       <li>OptimizationType.DISCRETE: each point is and the position of the agent is used to directly evaluate the evaluation function</li>
	 * 	    </ul>
	 * </p>
	 */
	private int stepCircleResolution = 4;

	/**
	 * <p>
	 *     Parameter of the optimization method: the number of circles. Together with the {@link AttributesOSM#stepCircleResolution}
	 *     this gives the number of points used by the optimization solver.
	 * </p>
	 */
	private int numberOfCircles = 1;

	/**
	 * <p>
	 *     Parameter of the optimization method: Specifies the concrete optimization solver.
	 * </p>
	 */
	private OptimizationType optimizationType = OptimizationType.NELDER_MEAD;

	/**
	 * If true, introduced for every optimization process a random offset by which points will be shifted (on their circle).
	 * If false, there will be no random offset. In case {@link AttributesOSM#movementType} is not directional
	 * and the first point of each circle will at (r * cos(0), r * sin(0)).
	 */
	private boolean varyStepDirection = true;

	/**
	 * This should only be used if {@link OptimizationType} is equal <tt>DISCRETE</tt> or <tt>PSO</tt>, since all other optimization (on the disc) do not
	 * use this parameter. Reduces the circles of the optimization to a segments lying inside a cone (see seitz-2016 page 76).
	 * This does not effect the number of used points. The shape of the cone is computed by the formula in seitz-2016 which
	 * depends on the current velocity of the agent.
	 */
	private MovementType movementType = MovementType.ARBITRARY;

	/**
	 * Used to compute the desired step length which is {@link AttributesOSM#stepLengthIntercept} + {@link AttributesOSM#stepLengthSlopeSpeed} * speed.
	 * (see seitz-2016 page 71 or seitz-2012).
	 */
	private double stepLengthIntercept = 0.4625;

	/**
	 * Used to compute the desired step length which is {@link AttributesOSM#stepLengthIntercept} + {@link AttributesOSM#stepLengthSlopeSpeed} * speed + error
	 * (see seitz-2016 page 71 or seitz-2012).
	 */
	private double stepLengthSlopeSpeed = 0.2345;

	/**
	 * Used to compute the error term of the desired step length i.e. the standard deviation of the normal
	 * distribution which is the distribution of the error variable.
	 * (see seitz-2016 page 71 or seitz-2012).
	 */
	private double stepLengthSD = 0.036;

	/**
	 * Only used if {@link OptimizationType} is equal <tt>DISCRETE</tt> or <tt>PSO</tt>. If the potential does not improve by this
	 * movementThreshold, the agent will not move. This is in some sense similar to the effect of {@link AttributesOSM#minStepLength}.
	 */
	private double movementThreshold = 0;

	/**
	 * Only used if {@link AttributesOSM#minimumStepLength} is <tt>true</tt>. The agent will not move if the
	 * next improvement is less than {@link AttributesOSM#minStepLength} away from its current position.
	 * Furthermore, this will be ignored if an agent is on stairs.
	 */
	private double minStepLength = 0.10;

	/**
	 * If true enables the use of {@link AttributesOSM#minStepLength}. This attribute could be removed.
	 */
	private boolean minimumStepLength = true;

	/**
	 * The maximum amount of time a foot step of an agent can take. If the foot step takes more time
	 * its duration is reduced to {@link AttributesOSM#maxStepDuration}.
	 */
	private double maxStepDuration = Double.MAX_VALUE;

	/**
	 * SpeedAdjusters will only be active if this is <tt>true</tt>. For example this has to be true if the group model is
	 * active.
	 */
	private boolean dynamicStepLength = true;

	/**
	 * Specifies which update schema is used. The OSM should use the event driven update schema
	 * (see seitz-2014b)
	 */
	private UpdateType updateType = UpdateType.EVENT_DRIVEN;

	/**
	 * If true this avoids agent jumping over small walls. However, this does not fix the problem that
	 * the target potential computation fails due to small obstacles. Since this is a quick fix and the
	 * test is very expensive the default should be false!
	 */
	private boolean seeSmallWalls = false;


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

	/** Return a copy of the submodel class names. */
	public List<String> getSubmodels() {
		return new ArrayList<>(submodels);
	}

	public double getMinStepLength() {
		return minStepLength;
	}
}

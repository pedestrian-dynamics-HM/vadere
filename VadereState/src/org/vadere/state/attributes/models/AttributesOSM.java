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
 *     such as the Centroid Group Model which have their own attributes class such as {@link AttributesCGM}.
 *     Most of the parameters are used to configure the algorithm which computes the next position of an agent.
 * </p>
 * <p>
 * There exist different versions of the Optimal Steps Model which use different parameters such that not every parameter
 * is used for every version and some parameters are only used if some other parameter has a specific value. These different
 * versions can be found in the PhD thesis of Isabella von Sivers [4] and Michael Seitz [2].
 * </p>
 *
 * Each version of the OSM searches for the optimal next step by using different search algorithms / optimizers:
 * <ul>
 * <li>Discrete search on a circle: this is the original OSM formulation introduced in [1]. The step length is determined by the agents' speed (Eq. 6).
 *     The optimizer searches for the next best position on the circle at finite and equidistant points.
 * </li>
 * <li>Discrete search on a disc / cone: The above concept was extended to use multiple circles and, in addition, to use only points inside a cone
 *     which is defined by the speed of an agent such that agents avoid rapid directional changes.
 * </li>
 * <li>Continues search on a disc: Instead of evaluating a finite number of fixed points this optimizer searches on the whole disc such that agents
 *     use arbitrary
 * </li>
 * </ul>
 * <p>
 * The default version of the Optimal Steps Model is the one using the optimization on the disc and potential functions representing the personal
 * spaces see [1] or [5]. We deviate from [4] only in the concept of a minimal step length: In this implementation
 * an agent will not move if its next position is closer than its minimal step length ({@link AttributesOSM#minStepLength}).
 * </p>
 *
 * Parameters for the configuring the optimization method are:
 * <ul>
 *  <li>{@link AttributesOSM#stepCircleResolution}</li>
 *  <li>{@link AttributesOSM#numberOfCircles}</li>
 *  <li>{@link AttributesOSM#optimizationType}</li>
 *  <li>{@link AttributesOSM#varyStepDirection}</li>
 * </ul>
 * Dependent on the used combination, they have different meanings! The step length velocity correlation
 * <p>
 *     s(v) = {@link AttributesOSM#stepLengthIntercept} + {@link AttributesOSM#stepLengthSlopeSpeed} * v + error (Eq. 6)
 * </p>
 * discussed in [1].<br><br>
 * <b>Related publications:</b>
 * <ol>
 *     <li><a href="https://doi.org/10.1103/PhysRevE.86.046108">Natural discretization of pedestrian movement in continuous space</a></li>
 *     <li><a href="https://mediatum.ub.tum.de/?id=1293050">Simulating pedestrian dynamics: Towards natural locomotion and psychological decision making</a></li>
 *     <li><a href="https://doi.org/10.1088/1742-5468/2014/07/P07002">How update schemes influence crowd simulations</a></li>
 *     <li><a href="https://mediatum.ub.tum.de/doc/1303742/1303742.pdf">Modellierung sozialpsychologischer Faktoren in Personenstromsimulationen - Interpersonale Distanz und soziale Identit&auml;ten</a></li>
 *     <li><a href="https://doi.org/10.1016/j.trb.2015.01.009">Dynamic Stride Length Adaptation According to Utility And Personal Space</a></li>
 * </ol>
 */
@ModelAttributeClass
public class AttributesOSM extends Attributes {

	/**
	 * Parameter of the optimization solver method: the number of points on the most outer circle. The number of points on any other circle will be
	 * chosen based on the angle3D between two successive points on the most outer circle such that any angle3D between two successive points
	 * on any circle will be almost equal. Therefore the number of points on a circle decreases with its radius.
	 * The positioned points will be used in different ways which depends on the {@link OptimizationType}.
	 * <ul>
	 *  <li><tt>OptimizationType.NELDER_MEAD</tt> (default): each neighbouring pair of points and the agent position is used as a starting simplex</li>
	 *  <li><tt>OptimizationType.PSO</tt>: each point and the position of the agent is used as a starting position of a particle</li>
	 * 	<li><tt>OptimizationType.DISCRETE</tt>: each point and the position of the agent is used to directly evaluate the evaluation function (brute force)</li>
	 * </ul>
	 */
	private int stepCircleResolution = 4;

	/**
	 * Parameter of the optimization solver method: the number of circles. Together with the {@link AttributesOSM#stepCircleResolution}
	 * this gives the number of points used by the optimization solver. If r is the radius of the most outer circle and k is the number
	 * of circles the radii of the circles are r/k, 2 * r/k, ... (k-1) * r/k, r.
	 */
	private int numberOfCircles = 1;

	/**
	 * Parameter of the optimization method: Specifies the concrete optimization solver.
	 */
	private OptimizationType optimizationType = OptimizationType.NELDER_MEAD;

	/**
	 * If <tt>true</tt>, introduced for every optimization process a noise term by which points will be shifted (on their circle). See Eq. 4 in [1].
	 * If <tt>false</tt>, there will be no noise term which might lead to artifacts, especially in case of <tt>OptimizationType.DISCRETE</tt>.
	 * In that case and with {@link AttributesOSM#movementType} not <tt>DIRECTIONAL</tt>, the first point of each circle will at (r * cos(0), r * sin(0)).
	 */
	private boolean varyStepDirection = true;

	/**
	 * This has only an effect if {@link OptimizationType} is equal <tt>DISCRETE</tt> or <tt>PSO</tt>, since all other optimization (on the disc) do not
	 * use this parameter. Reduces the circles of the optimization to a segments lying inside a cone (see [2], page 76).
	 * This does not effect the number of used points. The shape of the cone is computed by Eq. 4.6, 4.7 which
	 * depends on the current velocity of the agent.
	 */
	private MovementType movementType = MovementType.ARBITRARY;

	/**
	 * Used to compute the desired step length which is {@link AttributesOSM#stepLengthIntercept} + {@link AttributesOSM#stepLengthSlopeSpeed} * speed, i.e.
	 * Eq. 6 in [1].
	 */
	private double stepLengthIntercept = 0.4625;

	/**
	 * Used to compute the desired step length which is {@link AttributesOSM#stepLengthIntercept} + {@link AttributesOSM#stepLengthSlopeSpeed} * speed + error, i.e.
	 * Eq. 6 in [1].
	 */
	private double stepLengthSlopeSpeed = 0.2345;

	/**
	 * Used to compute the error term of the desired step length i.e. the standard deviation of the normal
	 * distribution which is the distribution of the error variable (see Eq. 6 in [1]).
	 */
	private double stepLengthSD = 0.036;

	/**
	 * Only used if {@link OptimizationType} is equal <tt>DISCRETE</tt> or <tt>PSO</tt>. If the potential does not improve by this
	 * threshold, the agent will not move. This is in some sense similar to the effect of {@link AttributesOSM#minStepLength}.
	 */
	private double movementThreshold = 0;

	/**
	 * Only used if {@link AttributesOSM#minimumStepLength} is <tt>true</tt>. The agent will not move if the
	 * next improvement is less than {@link AttributesOSM#minStepLength} away from its current position.
	 * Furthermore, this will be ignored if an agent is on stairs.
	 */
	private double minStepLength = 0.10;

	/**
	 * If <tt>true</tt> enables the use of {@link AttributesOSM#minStepLength}. This attribute could be removed.
	 */
	private boolean minimumStepLength = true;

	/**
	 * The maximum amount of time a foot step of an agent can take. If the foot step takes more time
	 * its duration is reduced to {@link AttributesOSM#maxStepDuration}.
	 */
	private double maxStepDuration = Double.MAX_VALUE;

	/**
	 * <tt>SpeedAdjusters</tt> will only be active if this is <tt>true</tt>. For example this has to be true if the group model is
	 * active.
	 */
	private boolean dynamicStepLength = true;

	/**
	 * Specifies which update schema is used. The OSM should use the event driven update schema, see [3].
	 */
	private UpdateType updateType = UpdateType.EVENT_DRIVEN;

	/**
	 * If <tt>true</tt> this avoids agent jumping over small walls. However, this does not fix the problem that
	 * the target potential computation fails due to small obstacles. Since this is a quick fix and the
	 * test is very expensive the default should be false!
	 */
	private boolean seeSmallWalls = false;


	private String targetPotentialModel = "org.vadere.simulator.models.potential.fields.PotentialFieldTargetGrid";
	private String pedestrianPotentialModel = "org.vadere.simulator.models.potential.PotentialFieldPedestrianCompactSoftshell";
	private String obstaclePotentialModel = "org.vadere.simulator.models.potential.PotentialFieldObstacleCompactSoftshell";
	private List<String> submodels = new LinkedList<>();

	public AttributesOSM() {

	}

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

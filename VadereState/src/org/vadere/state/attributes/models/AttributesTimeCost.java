package org.vadere.state.attributes.models;

import org.vadere.state.attributes.Attributes;

/**
 * Provides attributes for a pedestrian, like body radius, height, gender...
 * Currently implemented: radius [m, default: 0.195].
 * 
 */
public class AttributesTimeCost extends Attributes {
	/**
	 * The different time cost function types that represents different scenario
	 * types.
	 * 
	 */
	public enum TimeCostFunctionType {
		/** a static middle scale navigation. */
		UNIT,
		/** a dynamic middle scale navigation (navigation around groups). */
		NAVIGATION,

		/** a dynamic middle scale navigation (queuing). */
		QUEUEING,

		/** for the queuing game */
		QUEUEING_GAME,

		/** for the queuing game */
		NAVIGATION_GAME,

		/**
		 * uses TimeCostObstacleDensity to get a smooth potential field around
		 * obstacles
		 */
		OBSTACLES
	}

	public enum LoadingType {
		/** use one single loading for all pedestrians. */
		CONSTANT,
		/**
		 * use c_D and c_D' for differ pedestrains of the some target and other
		 * target.
		 */
		CONSTANT_RESPECT_TARGETS,
		/**
		 * use the dynamic loading strategy to calculate for each pedestrain an
		 * individual loading.
		 */
		DYNAMIC,
		QUEUEGAME;
	}

	/** the standard derivation required for the gaussian method. */
	private double standardDerivation = 0.7;
	/** the method of density measurement. */
	private TimeCostFunctionType type = TimeCostFunctionType.UNIT;
	/** the weight of the density. */
	private double obstacleDensityWeight = 3.5;
	/**
	 * (hartmann-2012, c_D) = the weight of the pedestrian that has the some
	 * target as the potentialfield that will be generated.
	 */
	private double pedestrianSameTargetDensityWeight = 3.5;
	/**
	 * (hartmann-2012, c_D') = the weight of the pedestrian that has on other
	 * target than the potentialfield that will be generated.
	 */
	private double pedestrianOtherTargetDensityWeight = 3.5;
	/** the weight for the constant laoding c in hartmann-2012. */
	private double pedestrianWeight = 3.5;
	/**
	 * (queuing only) the factor that influences the queue width. = 1 > means
	 * lower width. The value should be in the intervall [1;infty[.
	 */
	// @SerializedName("queueWidthLaoding")
	private double queueWidthLoading = 1;

	/** the laoding that will be multiplied to the dynamic loading. */
	private double pedestrianDynamicWeight = 6.0;
	/**
	 * indicate that the dynamic potential field should calculate a individual
	 * loading for each pedestrian or uses a constant laoding strategy. This
	 * will be done by the loading strategies. If the
	 * loadingType=CONSTANT_RESPECT_TARGETS than
	 * pedestrianSameTargetDensityWeight and pedestrianOtherTargetDensityWeight
	 * has to be set. If loadingType=CONSTANT than pedestrianWeight has to be
	 * set.
	 */
	// @SerializedName("laodingType")
	private LoadingType loadingType = LoadingType.CONSTANT;

	// Getters...
	public double getStandardDerivation() {
		return standardDerivation;
	}

	public TimeCostFunctionType getType() {
		return type;
	}

	public double getObstacleDensityWeight() {
		return obstacleDensityWeight;
	}

	public double getPedestrianSameTargetDensityWeight() {
		return pedestrianSameTargetDensityWeight;
	}

	public double getPedestrianOtherTargetDensityWeight() {
		return pedestrianOtherTargetDensityWeight;
	}

	public double getPedestrianWeight() {
		return pedestrianWeight;
	}

	public double getQueueWidthLoading() {
		return queueWidthLoading;
	}

	public double getPedestrianDynamicWeight() {
		return pedestrianDynamicWeight;
	}

	public LoadingType getLoadingType() {
		return loadingType;
	}
}

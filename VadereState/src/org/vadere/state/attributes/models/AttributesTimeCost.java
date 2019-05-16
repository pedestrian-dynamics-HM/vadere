package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

/**
 * Provides all! parameters for all! time cost functions.
 *
 * TODO: split AttributesTimeCost timeCostAttributes into multiple classes see comment in {@link AttributesFloorField}
 */
@ModelAttributeClass
public class AttributesTimeCost extends Attributes {

	public enum LoadingType {
		/** use one single loading for all pedestrians. */
		CONSTANT,
		/**
		 * use c_D and c_D' for differ pedestrians of the some target and other
		 * target.
		 */
		CONSTANT_RESPECT_TARGETS,
		/**
		 * use the dynamic loading strategy to calculate for each pedestrian an
		 * individual loading.
		 */
		DYNAMIC,
		QUEUEGAME;
	}

	/** the standard derivation required for the gaussian method. */
	private double standardDeviation = 0.7;
	/** the method of density measurement. */
	private TimeCostFunctionType type = TimeCostFunctionType.UNIT;
	/** the weight of the density. */
	private double obstacleDensityWeight = 3.5;
	/**
	 * (hartmann-2012, c_D) = the weight of the pedestrian that has the some
	 * target as the potential field that will be generated.
	 */
	private double pedestrianSameTargetDensityWeight = 3.5;
	/**
	 * (hartmann-2012, c_D') = the weight of the pedestrian that has on other
	 * target than the potential field that will be generated.
	 */
	private double pedestrianOtherTargetDensityWeight = 3.5;
	/** the weight for the constant loading c in hartmann-2012. */
	private double pedestrianWeight = 3.5;
	/**
	 * (queueing only) the factor that influences the queue width. = 1 > means
	 * lower width. The value should be in the interval [1;infty[.
	 */
	// @SerializedName("queueWidthLoading")
	private double queueWidthLoading = 1;

	/** the loading that will be multiplied to the dynamic loading. */
	private double pedestrianDynamicWeight = 6.0;
	/**
	 * indicate that the dynamic potential field should calculate a individual
	 * loading for each pedestrian or uses a constant loading strategy. This
	 * will be done by the loading strategies. If the
	 * loadingType=CONSTANT_RESPECT_TARGETS than
	 * pedestrianSameTargetDensityWeight and pedestrianOtherTargetDensityWeight
	 * has to be set. If loadingType=CONSTANT than pedestrianWeight has to be
	 * set.
	 */
	// @SerializedName("loadingType")
	private LoadingType loadingType = LoadingType.CONSTANT;

	/**
	 * only used in TimeCostFunctionObstacleDistance
	 */
	private double width = 0.2;

	/**
	 * only used in TimeCostFunctionObstacleDistance
	 */
	private double height = 1.0;

	// Getters...
	public double getWidth() {
		return width;
	}

	public double getHeight() {
		return height;
	}

	public double getStandardDeviation() {
		return standardDeviation;
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

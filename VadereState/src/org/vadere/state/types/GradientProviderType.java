package org.vadere.state.types;

/**
 * Holds all viable gradient provider types.
 * 
 */
public enum GradientProviderType {
	FLOOR_EIKONAL_DISCRETE,
	/**
	 * using discrete solution of the Eikonal equation,
	 * like with Fast Marching
	 */
	FLOOR_EUCLIDEAN_CONTINUOUS,
	/**
	 * using continuous solution with Euklidean
	 * distance to targets
	 */
	FLOOR_EUCLIDEAN_CONTINUOUS_MOLLIFIED,
	/**
	 * using continuous solution with
	 * Euklidean distance to targets, mollified so that the gradient is
	 * continuous
	 */

	OBSTACLE_CONTINUOUS,
	/** obstacle "forces", continuous */

	PEDESTRIAN_CYCLE_CONTINUOUS,
	/**
	 * uses PEDESTRIAN_CONTINUOUS, but enables the
	 * user to define two teleporter lines where the pedestrians are set back to
	 * the respective other line.
	 */
	PEDESTRIAN_CONTINUOUS
	/** pedestran "forces", continuous */
}

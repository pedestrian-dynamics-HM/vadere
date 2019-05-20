package org.vadere.state.attributes.models;

/**
 * The different time cost function types that represents different scenario
 * types.
 *
 * @author Benedikt Zoennchen
 *
 */
public enum TimeCostFunctionType {
	/** a static middle scale navigation. */
	UNIT,
	/** a dynamic middle scale navigation (navigation around groups). */
	NAVIGATION,

	/** a dynamic middle scale navigation (queuing). */
	QUEUEING,

	/** for the queueing game */
	QUEUEING_GAME,

	/** for the queueing game */
	NAVIGATION_GAME,

	/**
	 * uses TimeCostObstacleDensity to get a smooth potential field around
	 * obstacles
	 */
	OBSTACLES,

	DISTANCE_TO_OBSTACLES
}
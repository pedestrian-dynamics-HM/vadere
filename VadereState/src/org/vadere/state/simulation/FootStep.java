package org.vadere.state.simulation;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * A foot step is a simple java bean which represents one pedestrian foot step which is defined by
 * <ol>
 *     <li>start time (simulation time in seconds)</li>
 *     <li>end time (simulation time in seconds)</li>
 *     <li>start point</li>
 *     <li>end point</li>
 * </ol>
 *
 * @author Benedikt Zoennchen
 */
public final class FootStep {

	/*
	 * These are not final because of Jackson.
	 */
	private double startTime;
	private double endTime;
	private VPoint start;
	private VPoint end;

	/**
	 * Constructor for Jackson.
	 */
	public FootStep() {}

	/**
	 * Default constructor.
	 *
	 * @param start     start point of the foot step
	 * @param end       end point of the foot step
	 * @param startTime start time of the foot step
	 * @param endTime   end time of the foot step
	 */
	public FootStep(@NotNull final VPoint start, @NotNull final VPoint end, final double startTime, final double endTime) {
		this.start = start;
		this.end = end;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public double getStartTime() {
		return startTime;
	}

	public VPoint getEnd() {
		return end;
	}

	public VPoint getStart() {
		return start;
	}
}

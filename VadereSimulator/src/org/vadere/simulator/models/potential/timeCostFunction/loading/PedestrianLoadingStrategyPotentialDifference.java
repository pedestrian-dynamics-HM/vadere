package org.vadere.simulator.models.potential.timeCostFunction.loading;

import java.util.HashMap;
import java.util.Map;

import org.vadere.simulator.models.potential.fields.IPotentialFieldTargetGrid;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * The PedestrianLoadingStrategyPotentialDifference calculates an individual
 * laoding for each body of the floor based on target potential differences. The
 * central idea is to calculate the potential difference on a potential field
 * the body doesnt use. The value is a measure of the repulsion effect of this
 * body with respect to all pedestrians that uses the potential field that uses
 * this laoding strategy. A negative potential difference indicates that the
 * body does not move towards the target, so the laoding should be higher. If
 * the potential difference is zero the body does not move. The Problem is that
 * a negative potential difference on the static potential field does not
 * necesserely mean that the pedestrian disturbs. On the dynamic field on the
 * other hand we have no well defined measurement. So we try to combine the
 * informations. The dynamic field gives the indication that the movement is
 * disturbing (if the potential difference is negative on the dynamic field) or
 * is not disturbing (if the potential is positive). The static field gives the
 * us the measurement.
 * 
 *
 *         // TODO [priority=medium] [task=refactoring] Remove this class
 * 
 */
@Deprecated
class PedestrianLoadingStrategyPotentialDifference implements
		IPedestrianLoadingStrategy {
	private final double loading;
	private final double meanSpeed;
	private final int targetId;
	private final Topography floor;
	private final double divider;
	private final IPotentialFieldTargetGrid baseField;
	private final Map<Integer, VPoint> prevPedestrianPositions;
	private final double EPSILON = 0.0000001;

	/**
	 * Construct a new PedestrianLoadingStrategyPotentialDifference object.
	 * 
	 * @param floor
	 *        the floor that contains all the pedestrians that count
	 * @param targetId
	 *        the target id of the potential field that will be influenced
	 *        by this loadin strategy
	 * @param loading
	 *        the constant laoding that will be multiplied to the dynamic
	 *        laoding
	 * @param meanSpeed
	 *        the mean speed of all pedestrians
	 * @param baseField
	 */
	PedestrianLoadingStrategyPotentialDifference(
			final Topography floor,
			final int targetId,
			final double loading,
			final double meanSpeed,
			final IPotentialFieldTargetGrid baseField) {
		this.loading = loading;
		this.meanSpeed = meanSpeed;
		this.targetId = targetId;
		this.floor = floor;
		this.divider = meanSpeed * meanSpeed / Math.log(0.5);
		this.baseField = baseField;
		this.baseField.preLoop(0.0);
		this.baseField.update(0.0);
		this.prevPedestrianPositions = new HashMap<>();
	}

	@Override
	public double calculateLoading(final Pedestrian pedestrian) {
		if (pedestrian.getVelocity().getLength() > EPSILON) {
			System.out.println("computeGodunovDifference: " + pedestrian.getVelocity().getLength());
			return 0.0;
		} else {
			return loading;
		}
	}

	@Override
	public String toString() {
		return "\n" + loading + " * 2 * " + meanSpeed + " * exp^((speed+"
				+ meanSpeed + ")/" + meanSpeed
				+ "+)^2 * ln(0.5)),     if x >= " + (-meanSpeed) + " \n"
				+ +loading + " * 2 * " + meanSpeed + "                    else";
	}
}

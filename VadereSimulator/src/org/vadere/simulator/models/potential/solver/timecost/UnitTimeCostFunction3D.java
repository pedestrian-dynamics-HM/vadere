package org.vadere.simulator.models.potential.solver.timecost;

import org.vadere.util.geometry.Vector3D;

/**
 * Provides unit (= 1) cost at every point in 3D space.
 * 
 */
public class UnitTimeCostFunction3D implements ITimeCostFunction3D {

	/**
	 * Returns one, independent of p.
	 * 
	 * @param p
	 *        point in space, ignored.
	 * @return one, independent of p.
	 */
	@Override
	public double costAt(Vector3D p) {
		return 1;
	}

}

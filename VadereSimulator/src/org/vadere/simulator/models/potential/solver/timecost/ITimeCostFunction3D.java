package org.vadere.simulator.models.potential.solver.timecost;

import org.vadere.util.geometry.Vector3D;

/**
 * Interface for a generic time cost function in 3D.
 * 
 * 
 */
public interface ITimeCostFunction3D {

	/**
	 * Computes a generic, double-valued cost at a given point in 3D space.
	 * 
	 * @param p
	 *        a point in 3D space.
	 * @return the double-valued cost at p.
	 */
	double costAt(Vector3D p);

}

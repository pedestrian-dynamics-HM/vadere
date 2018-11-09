package org.vadere.simulator.models.potential.solver.timecost;

import org.vadere.util.geometry.shapes.IPoint;

/**
 * Provides unit (= 1) cost at every point in 2D space.
 * 
 */
public class UnitTimeCostFunction implements ITimeCostFunction {

	/**
	 * Returns one, independent of p.
	 * 
	 * @param p
	 *        point in space, ignored.
	 * @return one, independent of p.
	 */
	@Override
	public double costAt(IPoint p) {
			return 1;
	}

	@Override
	public void update() {}

	@Override
	public boolean needsUpdate() {
		return false;
	}

    @Override
    public ITimeCostFunction clone() {
        return new UnitTimeCostFunction();
    }

}

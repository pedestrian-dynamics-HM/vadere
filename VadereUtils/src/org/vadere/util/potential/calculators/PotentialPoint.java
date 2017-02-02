package org.vadere.util.potential.calculators;

import org.vadere.util.geometry.shapes.IPoint;

public interface PotentialPoint extends IPoint {
	double getPotential();
	void setPotential(final double potential);
	boolean isFrozen();
	void freeze();
}

package org.vadere.util.potential.calculators;

import org.vadere.geometry.shapes.IPoint;
import org.vadere.util.potential.PathFindingTag;

public interface IPotentialPoint extends IPoint {
	double getPotential();
	void setPotential(final double potential);
	void setPathFindingTag(PathFindingTag tag);
	PathFindingTag getPathFindingTag();
}

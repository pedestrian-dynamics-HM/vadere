package org.vadere.util.data.cellgrid;

import org.vadere.util.geometry.shapes.IPoint;

public interface IPotentialPoint extends IPoint {
	double getPotential();
	void setPotential(final double potential);
	void setPathFindingTag(PathFindingTag tag);
	PathFindingTag getPathFindingTag();
}

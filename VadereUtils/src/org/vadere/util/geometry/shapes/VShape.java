package org.vadere.util.geometry.shapes;

import java.awt.Shape;

import org.vadere.util.geometry.ShapeType;

/**
 * Geometric shape and position.
 * 
 */
public interface VShape extends Shape, Cloneable {
	double distance(VPoint point);

	VPoint closestPoint(VPoint point);

	boolean contains(VPoint point);

	VShape translate(final VPoint vector);

	VShape translatePrecise(final VPoint vector);

	VShape scale(final double scalar);

	boolean intersects(VLine intersectingLine);

	VPoint getCentroid();

	ShapeType getType();
}

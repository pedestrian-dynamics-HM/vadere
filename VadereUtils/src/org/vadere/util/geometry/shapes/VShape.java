package org.vadere.util.geometry.shapes;

import java.awt.Shape;

import org.vadere.util.geometry.ShapeType;

/**
 * Geometric shape and position.
 * 
 */
public interface VShape extends Shape, Cloneable {
	double distance(IPoint point);

	VPoint closestPoint(IPoint point);

	boolean contains(IPoint point);

	VShape translate(final IPoint vector);

	VShape translatePrecise(final IPoint vector);

	VShape scale(final double scalar);

	boolean intersects(VLine intersectingLine);

	VPoint getCentroid();

	ShapeType getType();
}

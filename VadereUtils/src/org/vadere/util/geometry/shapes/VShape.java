package org.vadere.util.geometry.shapes;

import java.awt.Shape;
import java.awt.geom.Area;

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

	default boolean intersects(VShape shape){
		Area thisShape = new Area(this);
		Area otherShape = new Area(shape);
		Area thisShapeCpy = new Area(this);
		thisShape.subtract(otherShape);
		return !thisShape.equals(thisShapeCpy);
	}

	default boolean sameArea(VShape shape){
		Area thisShape = new Area(this);
		Area otherShape = new Area(shape);
		thisShape.subtract(otherShape);
		return thisShape.isEmpty();
	}

	default boolean containsShape(VShape otherShape){
		Area thisArea = new Area(this);
		Area otherArea = new Area(otherShape);
		thisArea.intersect(otherArea);
		return thisArea.equals(otherArea);

	}
}

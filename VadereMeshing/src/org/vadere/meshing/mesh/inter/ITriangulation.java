package org.vadere.meshing.mesh.inter;

import org.apache.commons.lang3.tuple.Triple;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.stream.Stream;

/**
 * A {@link ITriangulation} is a set of connected triangles.
 *
 * @param <P> the type of the points of the triangles
 */
public interface ITriangulation<P extends IPoint> {

	/**
	 * Returns a {@link Stream} of {@link VTriangle} which represent the triangles of this triangulation.
	 *
	 * @return a {@link Stream} of {@link VTriangle}
	 */
	default Stream<VTriangle> streamTriangles() {
		return streamTriples().map(tripple -> new VTriangle(
				new VPoint(tripple.getLeft()),
				new VPoint(tripple.getMiddle()),
				new VPoint(tripple.getRight())));
	}

	/**
	 * Returns a {@link Stream} of {@link Triple} of {@link P} which represent the triangles of this triangulation.
	 *
	 * @return a {@link Stream} of {@link Triple} of {@link P}
	 */
	Stream<Triple<P, P, P>> streamTriples();

	/**
	 * Returns a {@link Stream} of {@link P} which are the points of the triangulation.
	 *
	 * @return a {@link Stream} of {@link P} which are the points of the triangulation.
	 */
	Stream<P> streamPoints();
}

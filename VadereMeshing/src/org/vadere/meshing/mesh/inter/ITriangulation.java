package org.vadere.meshing.mesh.inter;

import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IVertex;
import org.vadere.meshing.mesh.inter.meshConnectivity.ITriConnectivity;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * A {@link ITriangulation} is a set of connected triangles.
 */
public interface ITriangulation<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	/**
	 * Inserts a new point into the mesh / triangulation by preserving a feasible triangulation. There are different possible
	 * outcomes:
	 * <ol>
	 *     <li>the face will be split by an edge-split {@link ITriConnectivity#splitEdge(IPoint, IHalfEdge)}</li>
	 *     <li>the face will be split by an face-split {@link ITriConnectivity#splitTriangle(IFace, IPoint)}</li>
	 *     <li>the point is very close to some point of the face {@link org.vadere.meshing.mesh.inter.meshConnectivity.IReadOnlyTriConnectivity#isClose(double, double, IFace, double)}
	 *     and therefore it will not be inserted at all.</li>
	 * </ol>
	 * This requires amortized O(1) time.
	 *
	 * <p>Assumption:  the face contains the point or the point lines on an edge of the face
	 *              and the face is part of the mesh.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param p     the point which will be inserted
	 * @param face  the face which contains the point.
	 * @return one of the new created half-edges
	 */
	E insert(@NotNull final IPoint p, @NotNull final F face);

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
	 * Returns a {@link Stream} of {@link Triple} of {@link IPoint} which represent the triangles of this triangulation.
	 *
	 * @return a {@link Stream} of {@link Triple} of {@link IPoint}
	 */
	Stream<Triple<IPoint, IPoint, IPoint>> streamTriples();

	/**
	 * Returns a {@link Stream} of {@link IPoint} which are the points of the triangulation.
	 *
	 * @return a {@link Stream} of {@link IPoint} which are the points of the triangulation.
	 */
	Stream<IPoint> streamPoints();


	Optional<F> locateFace(final IPoint point);

	Optional<F> locateFace(final double x, final double y);

	Optional<F> locateFace(@NotNull final IPoint point, final Object caller);

	Optional<F> locateFace(@NotNull final double x, final double y, final Object caller);
}

package org.vadere.meshing.mesh.triangulation.triangulator.inter;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * <p>A triangulator i.e. a triangle generator creates a triangulation using a certain strategy.
 * The strategy determines which point will be inserted at which time and at which position.
 * The algorithm which inserts the point is part of the {@link ITriangulator}</p>
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface ITriangulator<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> {

	/**
	 * <p>Returns the generated triangulation.</p>
	 *
	 * @return the generated triangulation
	 */
	default IIncrementalTriangulation<P, CE, CF, V, E, F> generate() {
		return generate(true);
	}

	IIncrementalTriangulation<P, CE, CF, V, E, F> getTriangulation();

	/**
	 * <p>Returns the generated triangulation.</p>
	 *
	 * @param finalize if true finalizes the triangulation i.e. removes all virtual vertices
	 *                 and triangles inside holes.
	 * @return the generated triangulation
	 */
	IIncrementalTriangulation<P, CE, CF, V, E, F> generate(boolean finalize);

	IMesh<P, CE, CF, V, E, F> getMesh();

	//TODO this should be in an abstract class and it might be slow!
	default void split(@NotNull final E segment, @NotNull final Collection<E> segments) {
		segments.remove(segment);
		segments.remove(getMesh().getTwin(segment));

		// add s1, s2
		VLine line = getMesh().toLine(segment);
		VPoint midPoint = line.midPoint();
		V vertex = getMesh().createVertex(midPoint.getX(), midPoint.getY());
		V v1 = getMesh().getVertex(segment);
		V v2 = getMesh().getTwinVertex(segment);

		// split s
		List<E> toLegalize = getTriangulation().splitEdgeAndReturn(vertex, segment, false);

		// update data structure: add s1, s2
		E e1 = getMesh().getEdge(vertex, v1).get();
		E e2 = getMesh().getEdge(vertex, v2).get();

		segments.add(e1);
		segments.add(getMesh().getTwin(e1));
		segments.add(e2);
		segments.add(getMesh().getTwin(e2));

		for(E e : toLegalize) {
			getTriangulation().legalize(e, vertex);
		}
	}
}

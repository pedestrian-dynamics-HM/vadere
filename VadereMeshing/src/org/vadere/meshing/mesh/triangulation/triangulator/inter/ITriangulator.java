package org.vadere.meshing.mesh.triangulation.triangulator.inter;

import org.apache.commons.lang3.tuple.Pair;
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

/**
 * <p>A triangulator i.e. a triangle generator creates a triangulation using a certain strategy.
 * The strategy determines which point will be inserted at which time and at which position.
 * The algorithm which inserts the point is part of the {@link ITriangulator}</p>
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface ITriangulator<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	/**
	 * <p>Returns the generated triangulation.</p>
	 *
	 * @return the generated triangulation
	 */
	default IIncrementalTriangulation<V, E, F> generate() {
		return generate(true);
	}

	IIncrementalTriangulation<V, E, F> getTriangulation();

	/**
	 * <p>Returns the generated triangulation.</p>
	 *
	 * @param finalize if true finalizes the triangulation i.e. removes all virtual vertices
	 *                 and triangles inside holes.
	 * @return the generated triangulation
	 */
	IIncrementalTriangulation<V, E, F> generate(boolean finalize);

	IMesh<V, E, F> getMesh();

	//TODO this should be in an abstract class and it might be slow!
	default V split(@NotNull final E segment, @NotNull final Collection<E> segments) {
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

		return vertex;
	}
}

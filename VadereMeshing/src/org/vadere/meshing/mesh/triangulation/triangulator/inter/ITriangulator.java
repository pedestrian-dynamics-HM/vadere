package org.vadere.meshing.mesh.triangulation.triangulator.inter;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;
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

	default ITriangleMesh<V, E, F> getMesh() {
		return getMeshBuilder().getMesh();
	}

	default IMeshDataStorage<V, E, F> getMeshDataStorage(){
		return getMeshBuilder().getDataStorage();
	}

	ITriangleMeshBuilder<V, E, F>  getMeshBuilder();

	//TODO this should be in an abstract class and it might be slow!
	default V split(@NotNull final E segment, @NotNull final Collection<E> segments) {
		var vertices = getMesh().vertices();
		var edges = getMesh().edges();

		segments.remove(segment);
		segments.remove(edges.getTwin(segment));

		// add s1, s2
		VLine line = edges.toLine(segment);
		VPoint midPoint = line.midPoint();
		V vertex = getMeshBuilder().vertices().create(midPoint.getX(), midPoint.getY());
		V v1 = vertices.getEndOf(segment);
		V v2 = vertices.getTwin(segment);

		// split s
		List<E> toLegalize = getTriangulation().getMeshBuilder().changeConnectivity().splitEdgeAndReturn(vertex, segment, false);

		// update data structure: add s1, s2
		E e1 = edges.getOf(vertex, v1).get();
		E e2 = edges.getOf(vertex, v2).get();

		segments.add(e1);
		segments.add(edges.getTwin(e1));
		segments.add(e2);
		segments.add(edges.getTwin(e2));

		for(E e : toLegalize) {
			getTriangulation().getMeshBuilder().changeConnectivity().legalize(e, vertex);
		}

		return vertex;
	}
}

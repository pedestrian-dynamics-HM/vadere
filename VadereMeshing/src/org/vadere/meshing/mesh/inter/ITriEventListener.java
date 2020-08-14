package org.vadere.meshing.mesh.inter;

/**
 * A {@link ITriEventListener} listens to events that change the topography of the triangulation.
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface ITriEventListener<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	/**
	 * This method is triggered after a triangle is split into 3 new faces by inserting a
	 * point into the triangle.
	 *
	 * @param original  the original triangle / face
	 * @param f1        the first new triangle / face
	 * @param f2        the second new triangle / face
	 * @param f3        the third new triangle / face
	 * @param v         the new vertex
	 */
	default void postSplitTriangleEvent(F original, F f1, F f2, F f3, V v) {}

	/**
	 * This method is triggered after an half-edge is split. Note that if the half-edge is not
	 * at the boundary, one split of an edge will result in two splits of an half-edge. The original
	 * face might be reused for one of the new faces f1 or f2.
	 *
	 * @param originalEdge  original  the face of the half-edge
	 * @param original      the face of the half-edge
	 * @param f1            one of the new face, which might be the original
	 * @param f2            one of the new face, which might be the original
	 * @param v             the new vertex
	 */
	default void postSplitHalfEdgeEvent(E originalEdge, F original, F f1, F f2, V v) {}

	/**
	 * This method is triggered after an edge e with neighbouring faces f1, f2 is flipped.
	 *
	 * @param f1    the first face of the edge
	 * @param f2    the second face of the edge
	 */
	default void postFlipEdgeEvent(F f1, F f2) {}

	/**
	 * This method is triggered after a point is inserted into the triangulation.
	 *
	 * @param vertex the vertex of the point which was inserted
	 */
	default void postInsertEvent(V vertex) {}
}

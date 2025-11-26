package org.vadere.meshing.mesh.inter.meshConnectivity;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.IllegalMeshException;
import org.vadere.meshing.mesh.inter.ITriangleMeshPointLocator;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.mesh.builder.*;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshEdges;
import org.vadere.meshing.mesh.iterators.EdgeIterator;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * A poly-connectivity {@link IPolyConnectivity} is the connectivity of a mesh of non-intersecting connected polygons including holes.
 * So it is more abstract than a tri-connectivity {@link ITriConnectivity}. The mesh {@link IMesh} stores all the
 * date of the base elements, vertices {@link V}, half-edges {@link E} and faces {@link F}) and offers factory method
 * to create new base elements. The connectivities, i.e. {@link IPolyConnectivity} and {@link ITriConnectivity}
 * offers all the operations manipulating the connectivity of the mesh. The connectivity is the relation between vertices and edges which
 * define faces which therefore define the mesh structure.
 * </p>
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 *
 * @author Benedikt Zoennchen
 */
public interface IPolyConnectivity<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	/**
	 * <p>Returns the mesh builder of this poly-connectivity {@link IPolyConnectivity}.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @return the mesh of this IPolyConnectivity
	 */
	IMeshBuilder<V, E, F> getMeshBuilder();

	/**
	 * <p>Adjust the edge of a vertex in O(d) where d is the degree of the vertex.
	 * If there is an half-edge e which is at the boundary (i.e. hole or border) and has the vertex v
	 * as its end point, this method will set the half-edge of v to e. This is helpful to speed up the
	 * test whether a vertex is a boundary vertex!</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param vertex v
	 */
	void adjustVertex(@NotNull final V vertex);

	/**
	 * <p>Splits the edge (s) to (e) into two edges (s) to (p) to (e) in O(1).</p>
	 *
	 * <p>Changes the connectivity.</p>
	 *
	 * @param <V>   the type of the vertices
	 * @param <E>   the type of the half-edges
	 * @param <F>   the type of the faces
	 *
	 * @param edge  the edge
	 * @param p     the split point.
	 * @param meshBuilder  the mesh containing the edge and which will contain p afterwards
	 * @return returns the new vertex
	 */
	static <V extends IVertex, E extends IHalfEdge, F extends IFace> V splitEdge(
			@NotNull final E edge, @NotNull IPoint p, @NotNull ITriangleMeshBuilder<V, E, F> meshBuilder) {
		var edges = meshBuilder.getMesh().edges();
		var vertices = meshBuilder.getMesh().vertices();
		var faces = meshBuilder.getMesh().faces();
		var edgeBuilder = meshBuilder.edges();
		var vertexBuilder = meshBuilder.vertices();

		V u = meshBuilder.vertices().create(p);
		E twin = edges.getTwin(edge);
		E prev = edges.getPrev(edge);
		E tNext = edges.getNext(twin);

		E e = edgeBuilder.createAndInsert(u);
		edgeBuilder.setFace(e, faces.getOf(edge));
		E t = edgeBuilder.createAndInsert(vertices.getEndOf(twin));
		edgeBuilder.setFace(t, faces.getOf(twin));

		if(edges.getOf(vertices.getEndOf(twin)).equals(twin)) {
			vertexBuilder.setEdge(vertices.getEndOf(twin), t);
		}

		edgeBuilder.setVertex(twin, u);
		vertexBuilder.setEdge(u, e);

		edgeBuilder.setTwin(e, t);
		edgeBuilder.setNext(e, edge);
		edgeBuilder.setNext(twin, t);

		edgeBuilder.setPrev(e, prev);
		edgeBuilder.setNext(t, tNext);

		return u;
	}



	/**
	 * Splitting the face i.e. a polygon into as many faces as the face has edges which
	 * triangulates the face / polygon. This requires the time to locate the face which is O(n),
	 * where n is the number of faces for a basic implementation and O(log(n)) for more sophisticated
	 * point location algorithms see {@link ITriangleMeshPointLocator} and the actual split which
	 * requires O(1) but which might require additional changes e.g. in case for a Delaunay Triangulation
	 * see {@link IncrementalTriangulation}.
	 *
	 * Assumption: the vertex is valid i.e. it is contained in some face.
	 *
	 * Changes the connectivity.
	 *
	 * @param vertex the vertex which spilts the face which triangleContains the vertex. It has to be contained any face.
	 */
	void split(@NotNull final V vertex);

	/**
	 * Splitting the face i.e. a polygon into as many faces as the face has edges.
	 * This will essentially triangulate the polygon.
	 *
	 * Assumption: the vertex is valid i.e. it is contained in the face.
	 *
	 * Changes the connectivity.
	 *
	 * @param face      the face to be split into n faces, where n is the number of edges of the face
	 * @param vertex    the vertex which spilts the face. It has to be contained in the face
	 */
	void split(@NotNull final F face, @NotNull final V vertex);

	/**
	 * <p>Removes all links between the face and the otherFace. This essentially merges these two
	 * faces together if and only if there share a common edge. If one of these faces is the outer
	 * boundary i.e. the border the other one will be deleted. This requires O(max(n,m)), where n and m
	 * is the number of edges of the involved faces.</p>
	 *
	 * <p>Assumption: both faces aren't destroyed.</p>
	 *
	 * <p>Changes the connectivity.</p>
	 *
	 * @param face                      face one
	 * @param otherFace                 face two
	 * @param deleteIsolatedVertices    if true, vertices with degree zero will be removed from the mesh data structure otherwise they will not.
	 * @return  (optional) the remaining face (which might be face or otherFace)
	 *          or empty if both edges share no common edge and therefore nothing changes
	 */
	Optional<F> removeEdges(@NotNull final F face, @NotNull F otherFace, final boolean deleteIsolatedVertices);

	/**
	 * <p>A virus like working algorithm which merges neighbouring faces by starting at the face until
	 * the mergeCondition does no longer hold. This requires in the worst case O(n), where n is the number
	 * of edges of all involved faces (i.e. the face and the merged faces).</p>
	 *
	 * <p>Changes the connectivity.</p>
	 *
	 * @param face                      the face
	 * @param mergeCondition            the merge condition
	 * @param deleteIsolatedVertices    if true, vertices with degree zero will be removed from the mesh data structure otherwise they will not.
	 *
	 * @return the merge result i.e. the resulting face.
	 */
	default Optional<F> mergeFaces(@NotNull final F face, @NotNull final Predicate<F> mergeCondition, final boolean deleteIsolatedVertices) {
		return mergeFaces(face, mergeCondition, deleteIsolatedVertices, -1);
	}

	/**
	 * <p>A virus like working algorithm which merges neighbouring faces by starting at the face until
	 * the mergeCondition does no longer hold or the maximal dept is reached.
	 * This requires in the worst case O(n), where n is the number of edges of all involved faces
	 * (i.e. the face and the merged faces).</p>
	 *
	 * <p>Changes the connectivity.</p>
	 *
	 * @param face                      the face
	 * @param mergeCondition            the merge condition
	 * @param deleteIsolatedVertices    if true, vertices with degree zero will be removed from the mesh data structure otherwise they will not.
	 * @param errorCondition            a predicate which indicates that the merge process did merge faces which should not be merged,
	 *                                  and therefore leading to an illegal mesh. If this condition is ever satisfied an exception will be thrown.
	 * @throws IllegalMeshException     if during the merging <tt>errorCondition</tt> is true.
	 *
	 * @return the merge result i.e. the resulting face.
	 */
	default Optional<F> mergeFaces(
			@NotNull final F face,
			@NotNull final Predicate<F> mergeCondition,
			@NotNull final Predicate<F> errorCondition,
			final boolean deleteIsolatedVertices) throws IllegalMeshException {
		return mergeFaces(face, mergeCondition, errorCondition, deleteIsolatedVertices, -1);
	}

	/**
	 * <p>A virus like working algorithm which merges neighbouring faces by starting at the face until
	 * the mergeCondition does no longer hold or the maximal dept is reached.
	 * This requires in the worst case O(n), where n is the number of edges of all involved faces
	 * (i.e. the face and the merged faces).</p>
	 *
	 * <p>Changes the connectivity.</p>
	 *
	 * @param face                      the face
	 * @param mergeCondition            the merge condition
	 * @param deleteIsolatedVertices    if true, vertices with degree zero will be removed from the mesh data structure otherwise they will not.
	 * @param maxDept                   the maximum dept / neighbouring distance at which faces can be removed
	 *
	 * @return the merge result i.e. the resulting face.
	 */
	default Optional<F> mergeFaces(
			@NotNull final F face,
			@NotNull final Predicate<F> mergeCondition,
			final boolean deleteIsolatedVertices,
			final int maxDept) {
		try {
			return mergeFaces(face, mergeCondition, f -> false, deleteIsolatedVertices, maxDept);
		} catch (IllegalMeshException e) {
			e.printStackTrace();
		}
		// this will never happen.
		return Optional.empty();
	}

	// TODO: improve performance by remembering faces
	/**
	 * <p>A virus like working algorithm which merges neighbouring faces by starting at the face until
	 * the mergeCondition does no longer hold or the maximal dept is reached.
	 * This requires in the worst case O(n), where n is the number of edges of all involved faces
	 * (i.e. the face and the merged faces).</p>
	 *
	 * <p>Changes the connectivity.</p>
	 *
	 * @param face                      the face
	 * @param mergeCondition            the merge condition
	 * @param deleteIsolatedVertices    if true, vertices with degree zero will be removed from the mesh data structure otherwise they will not.
	 * @param maxDept                   the maximum dept / neighbouring distance at which faces can be removed
	 * @param errorCondition            a predicate which indicates that the merge process did merge faces which should not be merged,
	 *                                  and therefore leading to an illegal mesh. If this condition is ever satisfied an exception will be thrown.
	 * @throws IllegalMeshException     if during the merging <tt>errorCondition</tt> is true.
	 *
	 * @return the merge result i.e. the resulting face.
	 */
	Optional<F> mergeFaces(
			@NotNull final F face,
			@NotNull final Predicate<F> mergeCondition,
			@NotNull final Predicate<F> errorCondition,
			final boolean deleteIsolatedVertices,
			final int maxDept) throws IllegalMeshException;

	/**
	 * Creates a new hole or extends an existing hole by removing neighbouring faces by a virus algorithm
	 * which consumes faces as long as the merge condition holds.
	 *
	 * Changes the connectivity.
	 *
	 * @param face                      they face which will be transformed into a hole
	 * @param mergeCondition            the merge condition
	 * @param deleteIsoletedVertices    if true isolated vertices, i.e. vertices without any edges, will be removed from the mesh
	 * @param vertexAdjust              true means that boundary vertices will get their boundary edge as edge, false means there is no guarantee that this adjustment is made
	 * @return  (optional) the hole or face itself it the face does not fulfill the merge condition
	 *          or empty if due to the creation of the hole all faces will be removed!
	 */
	Optional<F> createHole(@NotNull final F face, @NotNull final Predicate<F> mergeCondition, final boolean deleteIsoletedVertices, final boolean vertexAdjust);

	default Optional<F> createHole(@NotNull final F face, @NotNull final Predicate<F> mergeCondition, final boolean deleteIsoletedVertices) {
		return createHole(face, mergeCondition, deleteIsoletedVertices, true);
	}

	/**
	 * Shrinks the border as long as the removeCondition is satisfied i.e. a face will be removed if
	 * it is at the border (during the shrinking process) and satisfies the condition. Like a virus this
	 * algorithms removes faces from outside, i.e. the border, towards inside. This requires O(n)
	 * where n is the number of edges of all involved faces (the border and the removed ones).
	 *
	 * Changes the connectivity.
	 *
	 * @param removeCondition           the remove condition
	 * @param deleteIsolatedVertices    true then isolated vertices (they are not connected to an edge) will be removed.
	 * @param vertexAdjust              true means that boundary vertices will get their boundary edge as edge, false means there is no guarantee that this adjustment is made
	 */
	void shrinkBorder(final Predicate<F> removeCondition, final boolean deleteIsolatedVertices, final boolean vertexAdjust);

	void shrinkBorder(final Predicate<F> removeCondition, final boolean deleteIsolatedVertices);

	void shrinkBoundary(final Predicate<F> removeCondition, final boolean deleteIsolatedVertices);

	/**
	 * Shrinks the border as long as the removeCondition is satisfied i.e. a face will be removed if
	 * it is at the border (during the shrinking process) and satisfies the condition. Like a virus this
	 * algorithms removes faces from outside, i.e. the border, towards inside. This requires O(n)
	 * where n is the number of edges of all involved faces (the border and the removed ones).
	 *
	 * Changes the connectivity.
	 *
	 * @param removeCondition           the remove condition
	 * @param deleteIsolatedVertices    true then isolated vertices (they are not connected to an edge) will be removed.
	 * @param adjustVertices            true means that boundary vertices will get their boundary edge as edge, false means there is no guarantee that this adjustment is made
	 */
	void shrinkBoundary(@NotNull final F boundary, final Predicate<F> removeCondition, final boolean deleteIsolatedVertices, final boolean adjustVertices);

	default void shrinkBoundary(@NotNull final F boundary, final Predicate<F> removeCondition, final boolean deleteIsolatedVertices) {
		shrinkBoundary(boundary, removeCondition, deleteIsolatedVertices, true);
	}

	void removeFacesAtBoundary(@NotNull final Predicate<F> mergePredicate, @NotNull final Predicate<F> errorPredicate) throws IllegalMeshException;

	/**
	 * Removes a simple link. This will be done by merging two faces into one remaining face. One of
	 * the face will be destroyed and the other one returned.
	 *
	 * Assumption: the edge is a simple link, if this is not the case the method will not change
	 * the topology.
	 *
	 * Changes the connectivity.
	 *
	 * @param edge the simple link
	 * @return the remaining face
	 */
	F removeEdgeSafely(@NotNull final E edge);

	/**
	 * Removes a simple link. This will be done by merging two faces into one remaining face. One of
	 * the face will be destroyed and the other one returned. This requires O(n) where n is the sum of
	 * the number of edges of both neighbouring faces.
	 *
	 * Assumption: the edge is a simple link
	 *
	 * Changes the connectivity.
	 *
	 * @param edge the simple link
	 * @return the remaining face
	 */
	F removeSimpleLink(@NotNull final E edge);

	/**
	 * <p>Removes a face from the mesh by removing all boundary edges of the face.
	 * If there is no boundary edge this method will not change the mesh topology.
	 * This requires O(n) (if the face is no island) where n is the number of edges of
	 * the face. If the face is an island (a very special case) this can require O(m), where m is the number of
	 * all edges of the mesh!</p>
	 *
	 * <p>Changes the connectivity.</p>
	 *
	 * <p>Assumption: boundary is in fact a boundary and is neighbouring the face and there is no other neighbouring boundary.</p>
	 *
	 * @param face                      the face that will be removed from the mesh
	 * @param boundary                  the boundary which has to be a neighbouring boundary of the face
	 * @param deleteIsolatedVertices    true means that all vertices with degree smaller equals 1 will be removed as well
	 * @param adjustVertices            true means that boundary vertices will get their boundary edge as edge, false means there is no guarantee that this adjustment is made
	 */
	void removeFaceAtBoundary(@NotNull final F face, @NotNull final F boundary, final boolean deleteIsolatedVertices, final boolean adjustVertices);


	default void removeFaceAtBoundary(@NotNull final F face, @NotNull final F boundary, final boolean deleteIsolatedVertices) {
		removeFaceAtBoundary(face, boundary, deleteIsolatedVertices, true);
	}

	/**
	 * Removes a face from the mesh by removing all boundary edges of the face.
	 * If there is no border edge this method will not change the mesh topology.
	 *
	 * Changes the connectivity.
	 *
	 * Assumption: the face is at the border
	 *
	 * @param face                      the face that will be removed from the mesh
	 * @param deleteIsolatedVertices    true means that all vertices with degree smaller equals 1 will be removed as well
	 */
	void removeFaceAtBorder(@NotNull final F face, final boolean deleteIsolatedVertices);

	/**
	 * <p>Inserts a point into the mesh which is contained in a boundary by connecting the boundaryEdge
	 * to the point in O(1) time. This will create 4 new half-edges, one new vertex and one face.</p>
	 *
	 * <p>Assumption: The point is contained in the boundary i.e. the point is inside the border or inside a hole.</p>
	 *
	 * <p>Changes the connectivity.</p>
	 *
	 * @param point         the point to be inserted
	 * @param boundaryEdge  the boundary edge
	 * @param boundary      the boundary of the edge
	 * @return the created face
	 */
	F insertOutsidePoint(@NotNull final IPoint point, @NotNull final E boundaryEdge, @NotNull final F boundary);
}

package org.vadere.util.geometry.mesh.inter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.gen.IncrementalTriangulation;
import org.vadere.util.geometry.mesh.iterators.EdgeIterator;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A poly-connectivity {@link IPolyConnectivity} is the connectivity of a mesh of non-intersecting connected polygons including holes.
 * So it is more abstract than a tri-connectivity {@link ITriConnectivity}. The mesh {@link IMesh} stores all the
 * date of the base elements (points {@link P}, vertices {@link V}, half-edges {@link E} and faces {@link F}) and offers factory method
 * to create new base elements. The connectivities, i.e. {@link IPolyConnectivity} and {@link ITriConnectivity}
 * offers all the operations manipulating the connectivity of the mesh. The connectivity is the relation between vertices and edges which
 * define faces which therefore define the mesh structure.
 *
 * @param <P> the type of the points (containers)
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 *
 * @author Benedikt Zoennchen
 */
public interface IPolyConnectivity<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Iterable<F>{

	/**
	 * A logger to debug some code.
	 */
	Logger log = LogManager.getLogger(IPolyConnectivity.class);

	/**
	 * Returns the mesh of this poly-connectivity {@link IPolyConnectivity<P, V, E, F>}.
	 *
	 * Does not change the connectivity.
	 *
	 * @return the mesh of this IPolyConnectivity
	 */
	IMesh<P, V, E, F> getMesh();

	default boolean isAtBoundary(@NotNull final E halfEdge) {
		IMesh<P, V, E, F> mesh = getMesh();
		return mesh.isBoundary(halfEdge) || mesh.isBoundary(mesh.getTwin(halfEdge));
	}

	/**
	 * Searches and returns the face containing the point (x,y) in O(n),
	 * where n is the number of faces of the mesh. For each polygon of the mesh
	 * the contained method will be evaluated until it returns <tt>true</tt> which
	 * is rather time consuming.
	 *
	 * Does not change the connectivity.
	 *
	 * @param x x-coordinate of the location point
	 * @param y y-coordinate of the location point
	 * @return the face containing the point or empty() if there is none
	 */
	default Optional<F> locateFace(final double x, final double y) {
		return getMesh().locate(x, y);
	}

	/**
	 * Searches and returns the face containing the point (x,y) in O(n),
	 * where n is the number of faces of the mesh.
	 *
	 * Does not change the connectivity.
	 *
	 * @param point the location point
	 * @return the face containing the point or empty() if there is none
	 */
	default Optional<F> locateFace(@NotNull final P point) {
		return locateFace(point.getX(), point.getY());
	}


	/**
	 * Adjust the edge of a vertex in O(d) where d is the degree of the vertex.
	 * If there is an half-edge e which is at the boundary (i.e. hole or border) and has the vertex v
	 * as its end point, this method will set the half-edge of v to e. This is helpful to speed up the
	 * test whether a vertex is a boundary vertex!
	 *
	 * Does not change the connectivity.
	 *
	 * @param vertex v
	 */
	default void adjustVertex(@NotNull final V vertex) {
		getMesh().streamEdges(vertex).filter(edge -> isAtBoundary(edge)).findAny().ifPresent(edge -> getMesh().setEdge(vertex, edge));
	}

	/**
	 * Returns a half-edge (begin, end) where end is its end point
	 * and begin is the end point of its predecessor. This requires
	 * O(d), where d is the degree of begin.
	 *
	 * Does not change the connectivity.
	 *
	 * @param begin the end point of the predecessor of the searched half-edge
	 * @param end   the end point of the searched half-edge
	 * @return a half-edge (begin, end) if there is any, otherwise empty()
	 */
	default Optional<E> findEdge(@NotNull final V begin, @NotNull final V end) {
		IMesh<P, V, E, F> mesh = getMesh();
		return mesh.getIncidentEdges(mesh.getEdge(begin)).stream()
				.filter(edge -> mesh.getPrev(edge).equals(end))
				.map(edge -> mesh.getTwin(edge)).findAny();
	}

	/**
	 * Tests if the half-edge is the only link / part of the full-edge
	 * between the face of the half-edge and the face of its twin.
	 * This requires O(n), where n is the number of edges of the face
	 * of the edge.
	 *
	 * Does not change the connectivity.
	 *
	 * @param halfEdge a half-edge to test
	 * @return true if the half-edge is a simple link, false otherwise
	 */
	default boolean isSimpleLink(@NotNull final E halfEdge) {
		E edge = halfEdge;
		E twin = getMesh().getTwin(halfEdge);
		F twinFace = getMesh().getFace(twin);

		E next = getMesh().getNext(edge);

		while (!edge.equals(next)) {
			if (twinFace.equals(getMesh().getTwinFace(next))) {
				return false;
			}
			next = getMesh().getNext(next);
		}
		return true;
	}

	/**
	 * Splits the edge (s)->(e)  into two edges (s)->(p)->(e) in O(1).
	 *                    <-                       <-   <-
	 *
	 * Changes the connectivity.
	 *
	 * @param edge  the edge
	 * @param p     the split point.
	 *
	 * @return returns the new vertex
	 */
	static <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> V splitEdge(
			@NotNull final E edge, @NotNull P p, @NotNull IMesh<P, V, E, F> mesh) {
		V u = mesh.createVertex(p);
		E twin = mesh.getTwin(edge);
		E prev = mesh.getPrev(edge);
		E tNext = mesh.getNext(twin);

		E e = mesh.createEdge(u);
		mesh.setFace(e, mesh.getFace(edge));
		E t = mesh.createEdge(mesh.getVertex(twin));
		mesh.setFace(t, mesh.getFace(twin));

		if(mesh.getEdge(mesh.getVertex(twin)).equals(twin)) {
			mesh.setEdge(mesh.getVertex(twin), t);
		}

		mesh.setVertex(twin, u);
		mesh.setEdge(u, e);

		mesh.setTwin(e, t);
		mesh.setNext(e, edge);
		mesh.setNext(twin, t);

		mesh.setPrev(e, prev);
		mesh.setNext(t, tNext);

		return u;
	}

	/**
	 * Tests if there is any face which shares more than one edge with the face
	 * we are checking. This requires O(n), where n is the number of edges of the face.
	 *
	 * Does not change the connectivity.
	 *
	 * @param face the face we are checking
	 * @return true if there is no face which shares more than one edge with this face, false otherwise
	 */
	default boolean isSimpleConnected(@NotNull final F face) {
		Set<F> faceSet = new HashSet<>();
		E edge = getMesh().getEdge(face);
		E next = getMesh().getNext(edge);
		faceSet.add(getMesh().getTwinFace(edge));

		while (!edge.equals(next)) {
			if(faceSet.contains(getMesh().getTwinFace(next))) {
				return false;
			}
			else {
				faceSet.add(getMesh().getTwinFace(next));
			}
			next = getMesh().getNext(next);
		}
		return true;
	}

	/**
	 * Splitting the face i.e. a polygon into as many faces as the face has edges which
	 * triangulates the face / polygon. This requires the time to locate the face which is O(n),
	 * where n is the number of faces for a basic implementation and O(log(n)) for more sophisticated
	 * point location algorithms see {@link IPointLocator<P, V, E, F>} and the actual split which
	 * requires O(1) but which might require additional changes e.g. in case for a Delaunay Triangulation
	 * see {@link IncrementalTriangulation <P, V, E, F>}.
	 *
	 * Assumption: the vertex is valid i.e. it is contained in some face.
	 *
	 * Changes the connectivity.
	 *
	 * @param vertex the vertex which spilts the face which triangleContains the vertex. It has to be contained any face.
	 */
	default void split(@NotNull final V vertex) {
		Optional<F> optFace = locateFace(getMesh().getPoint(vertex));
		if(!optFace.isPresent()) {
			throw new IllegalArgumentException(vertex + " is not contained in any face. Therefore, no face found to split into faces.");
		} else {
			split(optFace.get(), vertex);
		}
	}

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
	default void split(@NotNull final F face, @NotNull final V vertex) {
		assert locateFace(getMesh().getPoint(vertex)).get().equals(face);

		E hend = getMesh().getEdge(face);
		E hh = getMesh().getNext(hend);
		E hold = getMesh().createEdge(vertex);
		E twin = getMesh().createEdge(getMesh().getVertex(hend));

		getMesh().setTwin(hold, twin);
		getMesh().setNext(hend, hold);
		getMesh().setFace(hold, face);

		hold = getMesh().getTwin(hold);
		while (!hh.equals(hend)) {
			E hnext = getMesh().getNext(hh);
			F newFace = getMesh().createFace();
			getMesh().setEdge(newFace, hh);

			// update the edge of the vertex such that the last new created edge will be its edge
			E hnew = getMesh().createEdge(vertex);
			getMesh().setEdge(vertex, hnew);

			getMesh().setNext(hnew, hold);
			getMesh().setNext(hold, hh);
			getMesh().setNext(hh, hnew);

			getMesh().setFace(hnew, newFace);
			getMesh().setFace(hold, newFace);
			getMesh().setFace(hh, newFace);

			E hnewTwin = getMesh().createEdge(getMesh().getVertex(hh));
			getMesh().setTwin(hnew, hnewTwin);

			hold = hnewTwin;
			hh = hnext;
		}

		getMesh().setNext(hold, hend);
		getMesh().setNext(getMesh().getNext(hend), hold);
		getMesh().setFace(hold, face);
	}

	/**
	 * Removes all links between the face and the otherFace. This essentially merges these two
	 * faces together if and only if there share a common edge. If one of these faces is the outer
	 * boundary i.e. the border the other one will be deleted. This requires O(max(n,m)), where n and m
	 * is the number of edges of the involved faces.
	 *
	 * Assumption: both faces aren't destroyed.
	 *
	 * Changes the connectivity.
	 *
	 * @param face      face one
	 * @param otherFace face two
	 * @return the remaining face (which might be face or otherFace)
	 */
	default Optional<F> removeEdges(@NotNull final F face, @NotNull F otherFace, final boolean deleteIsolatedVertices) {
		// TODO: test it!
		assert !getMesh().isDestroyed(face) && !getMesh().isDestroyed(otherFace);

		F delFace = otherFace;
		F remFace = face;

		if(getMesh().isBoundary(delFace)) {
			F tmp = delFace;
			delFace = remFace;
			remFace = tmp;
		}

		if(getMesh().isBorder(delFace)) {
			F tmp = delFace;
			delFace = remFace;
			remFace = tmp;
		}

		final F finalFace = delFace;
		List<E> toDeleteEdges = getMesh().streamEdges(remFace).filter(e -> getMesh().getTwinFace(e).equals(finalFace)).collect(Collectors.toList());
		//List<E> survivalEdges = getMesh().streamEdges(remFace).filter(e -> !getMesh().getTwinFace(e).equals(finalFace)).collect(Collectors.toList());

		// face and otherFace share no common edge.
		if(toDeleteEdges.isEmpty()) {
			return Optional.empty();
		}

		for(E edge : toDeleteEdges) {
			E twin = getMesh().getTwin(edge);

			assert !getMesh().isDestroyed(delFace);

			E prevEdge = getMesh().getPrev(edge);
			E prevTwin = getMesh().getPrev(twin);

			E nextEdge = getMesh().getNext(edge);
			E nextTwin = getMesh().getNext(twin);

			// = prevEdge == twin
			boolean tDangling = nextTwin.equals(edge);

			// = prevTwin == edge
			boolean eDangling = nextEdge.equals(twin);

			// adjust vertices, mb later
			V eVertex = getMesh().getVertex(edge);
			V tVertex = getMesh().getVertex(twin);

			// twin vertex has to be deleted
			if(deleteIsolatedVertices && getMesh().getNext(twin).equals(edge)) {
				getMesh().destroyVertex(tVertex);
			}

			// edge vertex has to be deleted
			if(deleteIsolatedVertices && getMesh().getNext(edge).equals(twin)) {
				getMesh().destroyVertex(eVertex);
			}

			getMesh().setNext(prevEdge, nextTwin);
			getMesh().setNext(prevTwin, nextEdge);

			getMesh().setEdge(eVertex, prevTwin);
			getMesh().setEdge(tVertex, prevEdge);

			if(getMesh().getEdge(remFace).equals(edge)) {
				if(!eDangling) {
					getMesh().setEdge(remFace, nextEdge);
				}
				else {
					getMesh().setEdge(remFace, prevEdge);
				}
			}

			//getMesh().setEdge(remFace, survivalEdges.get(0));

			getMesh().destroyEdge(edge);
			getMesh().destroyEdge(twin);
		}

		for(E halfEdge : getMesh().getEdgeIt(remFace)) {
			getMesh().setFace(halfEdge, remFace);
			getMesh().setEdge(remFace, halfEdge);

			// adjust vertices to speed up isBoundary(vertex)
			/*if(getMesh().isBoundary(remFace)) {
				getMesh().setEdge(getMesh().getVertex(halfEdge), halfEdge);
			}*/
		}

		getMesh().destroyFace(delFace);

		return Optional.of(remFace);
	}

	// TODO: improve performance by remembering faces
	/**
	 * A virus like working algorithm which merges neighbouring faces by starting at the face until
	 * the mergeCondition does no longer hold. This requires in the worst case O(n), where n is the number
	 * of edges of all involved faces (i.e. the face and the merged faces).
	 *
	 * Changes the connectivity.
	 *
	 * @param face              the face
	 * @param mergeCondition    the merge condition
	 *
	 * @return the merge result i.e. the resulting face.
	 */
	default F mergeFaces(@NotNull final F face, @NotNull final Predicate<F> mergeCondition, final boolean deleteIsolatedVertices) {
		boolean modified = true;
		F currentFace = face;

		while (modified) {
			modified = false;

			List<F> neighbouringFaces = getMesh().getFaces(currentFace);
			for(F neighbouringFace : neighbouringFaces) {
				// the face might be destroyed by an operation before
				if(!getMesh().isDestroyed(neighbouringFace) && mergeCondition.test(neighbouringFace)) {
					Optional<F> optionalMergeResult = removeEdges(currentFace, neighbouringFace, deleteIsolatedVertices);

					if(optionalMergeResult.isPresent()) {
						modified = true;
						currentFace = optionalMergeResult.get();
					}
				}
			}
		}

		return currentFace;
	}

	/**
	 * Creates a new hole or extends an existing hole by removing neighbouring faces as
	 * long as the merge condition holds.
	 *
	 * Changes the connectivity.
	 *
	 * @param face                      they face which will be transformed into a hole
	 * @param mergeCondition            the merge condition
	 * @param deleteIsoletedVertices    if true isolated vertices, i.e. vertices without any edges, will be removed from the mesh
	 * @return the hole or face itself it the face does not fulfill the merge condition
	 */
	default F createHole(@NotNull final F face, @NotNull final Predicate<F> mergeCondition, final boolean deleteIsoletedVertices) {
		if(mergeCondition.test(face)) {
			F remainingFace = mergeFaces(face, mergeCondition, deleteIsoletedVertices);
			getMesh().toHole(remainingFace);
			return remainingFace;
		}
		else {
			return face;
		}
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
	 * @param deleteIsolatedVertices    true => isolated vertices (they are not connected to an edge) will be removed.
	 */
	default void shrinkBorder(final Predicate<F> removeCondition, final boolean deleteIsolatedVertices) {
		boolean modified = true;

		while (modified) {
			modified = false;
			List<F> neighbouringFaces = getMesh().getFaces(getMesh().getBorder());

			for(F neighbouringFace : neighbouringFaces) {
				// the face might be destroyed by an operation before
				if(!getMesh().isDestroyed(neighbouringFace) && removeCondition.test(neighbouringFace)) {
					removeFaceAtBorder(neighbouringFace, deleteIsolatedVertices);
					modified = true;
				}
			}
		}
	}

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
	default F removeEdgeSafely(@NotNull final E edge) {
		if (isSimpleLink(edge) && !getMesh().isDestroyed(edge)) {
			return removeSimpleLink(edge);
		}
		else {
			return getMesh().getFace(edge);
		}
	}

	/*default void removeEdgeUnsafe(@NotNull final E edge, final boolean deleteIsolatedVertices) {
		E twin = getMesh().getTwin(edge);
		E prevEdge = getMesh().getPrev(edge);
		E prevTwin = getMesh().getPrev(twin);
		E nextEdge = getMesh().getNext(edge);
		E nextTwin = getMesh().getNext(twin);

		// remove neighbouring faces
		//if(!getMesh().getFace(edge).equals(face)) {
		//	removeFace(getMesh().getFace(edge), true);
		//}

		//if(!getMesh().getFace(twin).equals(face)) {
		//	removeFace(getMesh().getFace(twin), true);
		//}


		// Link the from-side of the edge
		// off the model.

		V fromVertex = getMesh().getVertex(twin);
		E fromEdge = getMesh().getEdge(fromVertex);
		if(fromEdge.equals(twin)) {

			// isolated edge
			if(deleteIsolatedVertices && nextTwin.equals(edge)) {
				getMesh().destroyVertex(fromVertex);
			}
			else {
				getMesh().setEdge(fromVertex, prevEdge);
			}
		}

		V toVertex = getMesh().getVertex(edge);
		E toEdge =  getMesh().getEdge(toVertex);
		if(toEdge.equals(edge)) {

			// isolated edge
			if(deleteIsolatedVertices && prevEdge.equals(twin)) {
				getMesh().destroyVertex(toVertex);
			}
			else {
				getMesh().setEdge(toVertex, prevTwin);
			}
		}

		F edgeFace = getMesh().getFace(edge);
		F twinFace = getMesh().getFace(twin);
		E edgeFaceEdge = getMesh().getEdge(edgeFace);
		E twinFaceEdge = getMesh().getEdge(twinFace);

		if(edgeFace.equals(edgeFaceEdge)) {
			getMesh().setEdge(edgeFace, getMesh().getNext(edge));
		}

		if(twinFace.equals(twinFaceEdge)) {
			getMesh().setEdge(twinFace, getMesh().getPrev(twin));
		}

		getMesh().setNext(prevEdge, nextTwin);
		getMesh().setNext(prevTwin, nextEdge);

		getMesh().destroyEdge(edge);
		getMesh().destroyEdge(twin);

	}*/

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
	default F removeSimpleLink(@NotNull final E edge) {
		assert isSimpleLink(edge) && !getMesh().isDestroyed(edge);
		E twin = getMesh().getTwin(edge);
		F delFace = getMesh().getFace(edge);
		F remFace = getMesh().getFace(twin);

		if(getMesh().isBoundary(delFace)) {
			F tmp = delFace;
			delFace = remFace;
			remFace = tmp;
		}

		assert !getMesh().isDestroyed(delFace);

		E prevEdge = getMesh().getPrev(edge);
		E prevTwin = getMesh().getPrev(twin);

		E nextEdge = getMesh().getNext(edge);
		E nextTwin = getMesh().getNext(twin);

		getMesh().setNext(prevEdge, nextTwin);
		getMesh().setNext(prevTwin, nextEdge);

		// adjust vertices, mb later
		V eVertex = getMesh().getVertex(edge);
		V tVertex = getMesh().getVertex(twin);

		getMesh().setEdge(eVertex, prevTwin);
		getMesh().setEdge(tVertex, prevEdge);

		if(getMesh().getEdge(remFace).equals(edge)) {
			getMesh().setEdge(remFace, prevTwin);
		}
		else if(getMesh().getEdge(remFace).equals(twin)) {
			getMesh().setEdge(remFace, prevEdge);
		}

		for(E halfEdge : getMesh().getEdgeIt(remFace)) {
			getMesh().setFace(halfEdge, remFace);
			getMesh().setEdge(remFace, halfEdge);

			// adjust vertices to speed up isBoundary(vertex)
			/*if(getMesh().isBoundary(remFace)) {
				getMesh().setEdge(getMesh().getVertex(halfEdge), halfEdge);
			}*/
		}

		getMesh().destroyEdge(edge);
		getMesh().destroyEdge(twin);
		getMesh().destroyFace(delFace);

		return remFace;
	}

	/*default void remove(final V vertex, final boolean deleteIsolatedVertices) {
		List<F> toDeleteFaces = getMesh().getFaces(vertex);
		for(F face : toDeleteFaces) {
			removeFace(face, deleteIsolatedVertices);
		}
	}*/

	/**
	 * Tests whether the point (x, y) is contained in the face. This requires O(n), where
	 * n is the number of edges of the face.
	 *
	 * Assumption: The ordering of the mesh is CCW and the face is a valid simple polyon.
	 *
	 * Does not change the connectivity.
	 *
	 * @param x     x-coordinate of the point
	 * @param y     y-coordinate of the point
	 * @param face  the face
	 * @return true if the point (x, y) is contained in the face, false otherwise
	 */
	default boolean contains(final double x, final double y, final F face) {
		return getMesh().streamEdges(face).noneMatch(edge -> isRightOf(x, y, edge));
	}

	/**
	 * Returns true if the point (x1, y1) is part of the face in O(n),
	 * where n is the number of edges of the face.
	 *
	 * Does not change the connectivity.
	 *
	 * @param face  the face
	 * @param x1    the x-coordinate of the point
	 * @param y1    the y-coordinate of the point
	 * @return true if the (x1, y1) is part of the face, false otherwise
	 */
	default boolean isMember(final double x1, final double y1, final F face) {
		return getMemberEdge(face, x1, y1).isPresent();
	}

	/**
	 * Returns true if the point (x1, y1) is close to a point of the face in O(n),
	 * where n is the number of edges of the face.
	 *
	 * Does not change the connectivity.
	 *
	 * @param face      the face
	 * @param x1        the x-coordinate of the point
	 * @param y1        the y-coordinate of the point
	 * @param distance  the maximal distance
	 * @return true if the (x1, y1) close to a point of the face, false otherwise
	 */
	default boolean isClose(final double x1, final double y1, final F face, double distance) {
		return getCloseEdge(face, x1, y1, distance).isPresent();
	}

	/**
	 * (Optional) returns the half-edge of a face which ends in (x1, y1) in O(n),
	 * where n is the number of edges of the face.
	 *
	 * Does not change the connectivity.
	 *
	 * @param face  the face
	 * @param x1    the x-coordinate of the point
	 * @param y1    the y-coordinate of the point
	 * @return (optional) the half-edge of a face which ends in (x1, y1)
	 */
	default Optional<E> getMemberEdge(@NotNull final F face, final double x1, final double y1) {

		for(E e : getMesh().getEdgeIt(face)) {
			P p = getMesh().getPoint(e);
			if(p.getX() == x1 && p.getY() == y1) {
				return Optional.of(e);
			}
		}
		return Optional.empty();
	}

	/**
	 * (Optional) returns the half-edge of a face with an end-point close to (x1, y1) in O(n),
	 * where n is the number of edges of the face.
	 *
	 * Does not change the connectivity.
	 *
	 * @param face      the face
	 * @param x1        the x-coordinate of the point
	 * @param y1        the y-coordinate of the point
	 * @param distance  the maximal distance
	 * @return (optional) the half-edge of a face which ends in (x1, y1)
	 */
	default Optional<E> getCloseEdge(@NotNull final F face, double x1, double y1, double distance) {
		assert distance > 0;
		for(E e : getMesh().getEdgeIt(face)) {
			P p = getMesh().getPoint(e);
			if(p.distance(x1, y1) <= distance) {
				return Optional.of(e);
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns true if the point (x1, y1) is right of the half-edge in O(1). The half-edge is directed
	 * and ends in its point.
	 *
	 * Does not change the connectivity.
	 *
	 * @param x1    the x-coordinate of the point
	 * @param y1    the y-coordinate of the point
	 * @param edge  the half-edge
	 * @return true if the point (x1, y1) is right of the half-edge, false otherwise
	 */
	default boolean isRightOf(final double x1, final double y1, final E edge) {
		V v1 = getMesh().getVertex(getMesh().getPrev(edge));
		V v2 = getMesh().getVertex(edge);
		return GeometryUtils.isRightOf(v1.getX(), v1.getY(), v2.getX(), v2.getY(), x1, y1);
	}

	/**
	 * Returns true if the point (x1, y1) is left of the half-edge in O(1). The half-edge is directed
	 * and ends in its point.
	 *
	 * Does not change the connectivity.
	 *
	 * @param x1    the x-coordinate of the point
	 * @param y1    the y-coordinate of the point
	 * @param edge  the half-edge
	 * @return true if the point (x1, y1) is left of the half-edge, false otherwise
	 */
	default boolean isLeftOf(final double x1, final double y1, final E edge) {
		V v1 = getMesh().getVertex(getMesh().getPrev(edge));
		V v2 = getMesh().getVertex(edge);
		return GeometryUtils.isLeftOf(v1.getX(), v1.getY(), v2.getX(), v2.getY(), x1, y1);
	}

    /**
     * Tests if the line-segment defined by the half-edge intersects the line defined by p1 and p2 in O(1).
     *
     * Does not change the connectivity.
     *
     * @param p1    the first point of the undirected line
     * @param p2    the first point of the undirected line
     * @param edge  the half-edge defining the line-segment
     * @return true if the line-segment defined by the half-edge intersects the line (p1, p2)
     */
	default boolean intersects(final IPoint p1, final IPoint p2, E edge) {
		V v1 = getMesh().getVertex(getMesh().getPrev(edge));
		V v2 = getMesh().getVertex(edge);
		return GeometryUtils.intersectLine(p1.getX(), p1.getY(), p2.getX(), p2.getY(), v1.getX(), v1.getY(), v2.getX(), v2.getY());
	}


	/*default void fill_hole (final V v, final List<E> deletedEdges)
	{
		// uses the fact that the hole is starshaped
		// with repect to v->point()
		typedef std::list<Edge> Hole;

		Face_handle  ff, fn;
		int ii , in;
		Vertex_handle v0, v1, v2;
		Bounded_side side;

		//stack algorithm to create faces
		// create face v0,v1,v2
		//if v0,v1,v2 are finite vertices
		// and form a left_turn
		// and triangle v0v1v2 does not contain v->point()
		if( hole.size() != 3) {
			typename Hole::iterator hit = hole.begin();
			typename Hole::iterator next= hit;
			while( hit != hole.end() && hole.size() != 3) {
				ff = (*hit).first;
				ii = (*hit).second;
				v0 = ff->vertex(cw(ii));
				v1 = ff->vertex(ccw(ii));
				if( !is_infinite(v0) && !is_infinite(v1)) {
					next=hit; next++;
					if(next == hole.end()) next=hole.begin();
					fn = (*next).first;
					in = (*next).second;
					v2 = fn->vertex(ccw(in));
					if ( !is_infinite(v2) &&
							orientation(v0->point(), v1->point(), v2->point()) == LEFT_TURN ) {
						side =  bounded_side(v0->point(),
								v1->point(),
								v2->point(),
								v->point());

						if( side == ON_UNBOUNDED_SIDE ||
								(side == ON_BOUNDARY && orientation(v0->point(),
										v->point(),
										v2->point()) == COLLINEAR &&
										collinear_between(v0->point(),v->point(),v2->point()) ))
						{
							//create face
							Face_handle  newf = create_face(ff,ii,fn,in);
							typename Hole::iterator tempo=hit;
							hit = hole.insert(hit,Edge(newf,1)); //push newf
							hole.erase(tempo); //erase ff
							hole.erase(next); //erase fn
							if (hit != hole.begin() ) --hit;
							continue;
						}
					}
				}
				++hit;
			}
		}*/


	//TODO: not working as expected
	/**
	 * Removes a face from the mesh by removing all border edges of the face.
	 * If there are no border edges the face will be converted to be a hole. If
	 * there are any neighbouring holes, all of them will be merged together.
	 *
	 * Changes the connectivity.
	 *
	 * @param face                      the face that will be removed from the mesh
	 * @param deleteIsolatedVertices    true means that all vertices with degree <= 1 will be removed as well
	 */
	default void removeFace(@NotNull final F face, final boolean deleteIsolatedVertices) {
		throw new UnsupportedOperationException("doest not work correctly jet.");
		/*if(!getMesh().isDestroyed(face)) {
			List<F> mergeFaces = getMesh()
					.streamEdges(face)
					.map(e -> getMesh().getTwinFace(e))
					.filter(f -> getMesh().isHole(f))
					.distinct()
					.collect(Collectors.toList());



			F remainingFace = face;

			for(F nFace : mergeFaces) {
				remainingFace = removeEdges(remainingFace, nFace, deleteIsolatedVertices).orElse(remainingFace);
			}

			removeFaceAtBorder(remainingFace, deleteIsolatedVertices);
		}*/
	}

	/**
	 * Removes a face from the mesh by removing all boundary edges of the face.
	 * If there is no boundary edge this method will not change the mesh topology.
	 * This requires O(n) (if the face is no island) where n is the number of edges of
	 * the face. If the face is an island (a very special case) this can require O(m), where m is the number of
	 * all edges of the mesh!
	 *
	 * Changes the connectivity.
	 *
	 * Assumption: a neighbour of the face is the boundary and there is no other neighbouring boundary.
	 *
	 * @param face                      the face that will be removed from the mesh
	 * @param deleteIsolatedVertices    true means that all vertices with degree <= 1 will be removed as well
	 */
	default void removeFaceAtBoundary(@NotNull final F face, final F boundary, final boolean deleteIsolatedVertices) {
		if(!getMesh().isDestroyed(face)) {

			assert getMesh().streamFaces(face).filter(neighbour -> neighbour.equals(boundary)).count() > 0;

			List<E> delEdges = new ArrayList<>();
			List<V> vertices = new ArrayList<>();

			// number of edges of the face
			int nEdges = 0;
			boolean boundaryEdgeDeleted = false;
			E survivingEdge = null;
			E boundaryEdge = getMesh().getEdge(boundary);

			for(E edge : getMesh().getEdgeIt(face)) {
				E twin = getMesh().getTwin(edge);
				F twinFace = getMesh().getFace(twin);

				assert twinFace.equals(boundary) || !getMesh().isBoundary(twinFace);

				nEdges++;
				if(twinFace.equals(boundary)) {
					delEdges.add(edge);

					// adjust the boundary edge if it will be deleted
					if(boundaryEdge.equals(twin)) {
						boundaryEdgeDeleted = true;
					}
				}
				else {
					// remember an edge that will not be deleted. This edge can be used as the edge of the boundary.
					survivingEdge = edge;

					// if the edge will not be deleted it becomes an boundary edge
					getMesh().setFace(edge, boundary);
				}
				vertices.add(getMesh().getVertex(edge));
			}


			//TODO: this might be computational expensive!
			// special case: all edges will be deleted && the edge of the border will be deleted as well! => adjust the border edge
			if(getMesh().getTwinFace(boundaryEdge).equals(face) && delEdges.size() == nEdges) {
				assert survivingEdge == null;

				// all edges are border edges!
				EdgeIterator<P, V, E, F> edgeIterator = new EdgeIterator<>(getMesh(), boundaryEdge);

				F twinFace = getMesh().getTwinFace(boundaryEdge);

				// walk along the border away from this faces to get another edge which won't be deleted
				while (edgeIterator.hasNext() && twinFace.equals(face)) {
					boundaryEdge = edgeIterator.next();
					twinFace = getMesh().getTwinFace(boundaryEdge);
				}

				// no such candidate was found. This can happen if an island will be deleted.
				if(twinFace.equals(face)) {
					log.warn("no boundary candidate was found, we search through all edges of the mesh.");
					boundaryEdge = getMesh().streamEdges()
							.filter(e -> getMesh().getFace(e).equals(boundary))
							.filter(e -> !getMesh().getTwinFace(e).equals(face))
							.findAny().get();
				}

				getMesh().setFace(boundaryEdge, boundary);
				getMesh().setEdge(boundary, boundaryEdge);
			}
			else if(boundaryEdgeDeleted) {
				assert survivingEdge != null;
				getMesh().setEdge(boundary, survivingEdge);
			}

			if(!delEdges.isEmpty()) {
				E h0, h1, next0, next1, prev0, prev1;
				V v0, v1;

				for(E delEdge : delEdges) {
					h0 = delEdge;
					v0 = getMesh().getVertex(delEdge);
					next0 = getMesh().getNext(h0);
					prev0 = getMesh().getPrev(h0);

					h1    = getMesh().getTwin(delEdge);
					v1    = getMesh().getVertex(h1);
					next1 = getMesh().getNext(h1);
					prev1 = getMesh().getPrev(h1);

					boolean isolated0 = isSimpleConnected(v0);
					boolean isolated1 = isSimpleConnected(v1);

					//getMesh().setEdge(hole, prev1);

					// adjust next and prev half-edges
					getMesh().setNext(prev0, next1);
					getMesh().setNext(prev1, next0);

					//boolean isolated0 = getMesh().getNext(prev1).equals(getMesh().getTwin(prev1));
					//boolean isolated1 = getMesh().getNext(prev0).equals(getMesh().getTwin(prev0));

					//boolean isolated0 = getMesh().getTwin(h0) == getMesh().getNext(h0) || getMesh().getTwin(h0) == getMesh().getPrev(h0);
					//boolean isolated1 = getMesh().getTwin(h1) == getMesh().getNext(h1) || getMesh().getTwin(h1) == getMesh().getPrev(h1);

					// adjust vertices
					if(getMesh().getEdge(v0) == h0 && !isolated0) {
						getMesh().setEdge(v0, prev1);
					}

					if(deleteIsolatedVertices && isolated0) {
						getMesh().destroyVertex(v0);
					}

					if(getMesh().getEdge(v1) == h1 && !isolated1) {
						getMesh().setEdge(v1, prev0);
					}

					if(deleteIsolatedVertices && isolated1) {
						getMesh().destroyVertex(v1);
					}

					// mark edge deleted if the mesh has a edge status
					getMesh().destroyEdge(h0);
					getMesh().destroyEdge(h1);

					// adjust vertices such that we speed up the querry isBoundary(vertex).
					vertices.stream().filter(getMesh()::isAlive).forEach(v -> adjustVertex(v));
				}
			}

			if(nEdges > 0) {
				getMesh().destroyFace(face);
			}
			else {
				log.warn("could not delete face " + face + ". It is not at the border!");
			}

		}
	}


	/**
	 * Removes a face from the mesh by removing all border edges of the face.
	 * If there is no border edge this method will not change the mesh topology.
	 *
	 * Changes the connectivity.
	 *
	 * Assumption: the face is at the border
	 *
	 * @param face                      the face that will be removed from the mesh
	 * @param deleteIsolatedVertices    true means that all vertices with degree <= 1 will be removed as well
	 */
	default void removeFaceAtBorder(@NotNull final F face, final boolean deleteIsolatedVertices) {
		removeFaceAtBoundary(face, getMesh().getBorder(), deleteIsolatedVertices);
	}

	/**
	 * Tests whether the vertex has degree smaller or equals 2.
	 * If an edge gets deleted and the vertex is simple connected
	 * the vertex becomes isolated.
	 *
	 * Does not change the connectivity.
	 *
	 * @param vertex    the vertex
	 * @return true if the vertex has degree smaller or equals 2, false otherwise.
	 */
	default boolean isSimpleConnected(@NotNull final V vertex) {
		if(getMesh().isDestroyed(vertex)) {
			return true;
		}
		// test if degree of the vertex is <= 2
		E edge0 = getMesh().getEdge(vertex);
		E edge1 = getMesh().getTwin(getMesh().getNext(edge0));
		E edge2 = getMesh().getTwin(getMesh().getNext(edge1));
		return edge0 == edge1 || edge0 == edge2;
	}

	/**
	 * Returns a half-edge such that it is part of face1 and the twin of this half-edge
	 * is part of face2.
	 *
	 * Does not change the connectivity.
	 *
	 * @param face1 the first face
	 * @param face2 the second face that might be a neighbour of face1
	 * @return  the half-edge of face1 such that its twin is part of face2
	 */
	default Optional<E> findTwins(final F face1, final F face2) {
		for(E halfEdge1 : getMesh().getEdgeIt(face1)) {
			for(E halfEdge2 : getMesh().getEdgeIt(face2)) {
				if(getMesh().getTwin(halfEdge1).equals(halfEdge2)) {
					return Optional.of(halfEdge1);
				}
			}
		}
		return Optional.empty();
	}
}

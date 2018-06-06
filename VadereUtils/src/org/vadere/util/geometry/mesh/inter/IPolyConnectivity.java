package org.vadere.util.geometry.mesh.inter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.iterators.EdgeIterator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Benedikt Zoennchen
 */
public interface IPolyConnectivity<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Iterable<F>{

	Logger log = LogManager.getLogger(IPolyConnectivity.class);

	/**
	 * Returns the mesh of this IPolyConnectivity.
	 *
	 * Non mesh changing method.
	 *
	 * @return the mesh of this IPolyConnectivity
	 */
	IMesh<P, V, E, F> getMesh();

	default boolean isAtBoundary(@NotNull final E halfEdge) {
		IMesh<P, V, E, F> mesh = getMesh();
		return mesh.isBoundary(halfEdge) || mesh.isBoundary(mesh.getTwin(halfEdge));
	}

	/**
	 * Searches and returns the face containing the point (x,y).
	 *
	 * Non mesh changing method.
	 *
	 * @param x x-coordinate of the location point
	 * @param y y-coordinate of the location point
	 * @return the face containing the point or empty() if there is none
	 */
	default Optional<F> locateFace(final double x, final double y) {
		for(F face : getMesh().getFaces()) {
			VPolygon polygon = getMesh().toPolygon(face);
			if(polygon.contains(new VPoint(x, y))) {
				return Optional.of(face);
			}
		}
		return Optional.empty();
	}

	/**
	 * Searches and returns the face containing the point (x,y).
	 *
	 * Non mesh changing method.
	 *
	 * @param point the location point
	 * @return the face containing the point or empty() if there is none
	 */
	default Optional<F> locateFace(@NotNull final P point) {
		return locateFace(point.getX(), point.getY());
	}


	/**
	 * If there is an half-edge e which is at the boundary (i.e. hole or border) and has the vertex v
	 * as its end point, this method will set the half-edge of v to e.
	 *
	 * Non mesh changing method.
	 *
	 * @param vertex v
	 */
	default void adjustVertex(@NotNull final V vertex) {
		getMesh().streamEdges(vertex).filter(edge -> isAtBoundary(edge)).findAny().ifPresent(edge -> getMesh().setEdge(vertex, edge));
	}

	/**
	 * Returns a half-edge (begin, end) where end is its end point
	 * and begin is the end point of its predecessor.
	 * Non mesh changing method.
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
	 * Tests if the half-edge is the only link (of the face of the half-edge)
	 * between the face of the half-edge and the face of its twin.
	 *
	 * Non mesh changing method.
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
	 * Tests if there is any face which shares more than one edge with the face
	 * we are checking.
	 *
	 * Non mesh changing method.
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
	 * Splitting the face i.e. a polygon into as many faces as the face has edges.
	 *
	 * Assumption: the vertex is valid i.e. it is contained any face.
	 *
	 * Mesh changing method.
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
	 * Mesh changing method.
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
	 * boundary i.e. border the other one will be deleted.
	 *
	 * Assumption: both faces aren't destroyed.
	 *
	 * @param face      face one
	 * @param otherFace face two
	 * @return the remaining face (which might be face or otherFace)
	 */
	default F removeEdges(@NotNull final F face, @NotNull F otherFace, final boolean deleteIsolatedVertices) {
		// TODO: test it!
		assert !getMesh().isDestroyed(face) && !getMesh().isDestroyed(otherFace);

		F delFace = otherFace;
		F remFace = face;

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
			return face;
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
		}

		getMesh().destroyFace(delFace);

		return remFace;
	}

	/**
	 *
	 * @param face
	 * @param mergeCondition
	 * @return
	 */
	default F mergeFaces(F face, final Predicate<F> mergeCondition, final boolean deleteIsolatedVertices) {
		boolean modified = true;
		F currentFace = face;

		while (modified) {
			modified = false;

			for(F neighbouringFace : getMesh().getFaces(currentFace)) {
				// the face might be destroyed by an operation before
				if(!getMesh().isDestroyed(neighbouringFace) && mergeCondition.test(neighbouringFace)) {
					currentFace = removeEdges(currentFace, neighbouringFace, deleteIsolatedVertices);
					if(currentFace != null) {
						modified = true;
					}
				}
			}
		}

		return currentFace;
	}

	default void shrinkBorder(final Predicate<F> removeCondition, final boolean deleteIsolatedVertices) {
		boolean modified = true;

		while (modified) {
			modified = false;

			for(F neighbouringFace : getMesh().getFaces(getMesh().getBorder())) {
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
	 * Mesh changing method.
	 *
	 * @param edge the simple link
	 * @return the remaining face
	 */
	default F removeEdgeSafely(@NotNull final E edge) {
		if (isSimpleLink(edge) && !getMesh().isDestroyed(edge)) {
			return removeEdge(edge);
		}
		else {
			return getMesh().getFace(edge);
		}
	}

	/**
	 * Removes a simple link. This will be done by merging two faces into one remaining face. One of
	 * the face will be destroyed and the other one returned.
	 *
	 * Assumption: the edge is a simple link
	 *
	 * Mesh changing method.
	 *
	 * @param edge the simple link
	 * @return the remaining face
	 */
	default F removeEdge(@NotNull final E edge) {
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

	default boolean contains(final double x1, final double y1, final F face) {
		return getMesh().streamEdges(face).noneMatch(edge -> isRightOf(x1, y1, edge));
	}

	/*default boolean contains(final double x1, final double y1, final F face, boolean onBoundary) {
		if(!onBoundary) {
			return getMesh().streamEdges(face).noneMatch(edge -> isRightOf(x1, y1, edge));
		}
		else {

		}
	}*/

	default boolean isMember(final double x1, final double y1, final F face) {
		return getMemberEdge(face, x1, y1).isPresent();
	}

	default boolean isMember(final double x1, final double y1, final F face, double epsilon) {
		return getMemberEdge(face, x1, y1, epsilon).isPresent();
	}

	default Optional<E> getMemberEdge(@NotNull final F face, final double x1, final double y1) {
		for(E e : getMesh().getEdgeIt(face)) {
			P p = getMesh().getPoint(e);
			if(p.getX() == x1 && p.getY() == y1) {
				return Optional.of(e);
			}
		}
		return Optional.empty();
	}

	default Optional<E> getMemberEdge(@NotNull final F face, double x1, double y1, double epsilon) {
		assert epsilon > 0;
		for(E e : getMesh().getEdgeIt(face)) {
			P p = getMesh().getPoint(e);
			if(p.distance(x1, y1) <= epsilon) {
				return Optional.of(e);
			}
		}
		return Optional.empty();
	}

	default boolean isRightOf(final double x1, final double y1, final E edge) {
		VPoint p1 = getMesh().toPoint(getMesh().getVertex(getMesh().getPrev(edge)));
		VPoint p2 = getMesh().toPoint(getMesh().getVertex(edge));
		return GeometryUtils.isRightOf(p1, p2, x1, y1);
	}

	default boolean isLeftOf(final double x1, final double y1, final E edge) {
		VPoint p1 = getMesh().toPoint(getMesh().getVertex(getMesh().getPrev(edge)));
		VPoint p2 = getMesh().toPoint(getMesh().getVertex(edge));
		return GeometryUtils.isLeftOf(p1, p2, x1, y1);
	}

    /**
     * Tests if the line-segment edge intersects the line defined by p1 and p2.
     * @param p1
     * @param p2
     * @param edge
     * @return
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


	/**
	 * Removes a face from the mesh by removing all border edges of the face.
	 * If there are no border edges the face will be converted to be a hole. If
	 * there are any neighbouring holes, all of the will be merged together.
	 *
	 * Mesh changing method.
	 *
	 * @param face                      the face that will be removed from the mesh
	 * @param deleteIsolatedVertices    true means that all vertices with degree <= 1 will be removed as well
	 */
	default void removeFace(@NotNull final F face, final boolean deleteIsolatedVertices) {
		//TODO: not working as expected

		//if(!getMesh().getPoint(getMesh().getEdge(face)).equals(new VPoint(1.375, 0.0))) {
			if(!getMesh().isDestroyed(face)) {
				List<F> mergeFaces = getMesh()
						.streamEdges(face)
						.map(e -> getMesh().getTwinFace(e))
						.filter(f -> getMesh().isHole(f))
						.distinct()
						.collect(Collectors.toList());



				F remainingFace = face;

				for(F nFace : mergeFaces) {
					remainingFace = removeEdges(remainingFace, nFace, deleteIsolatedVertices);
				}

				removeFaceAtBorder(remainingFace, deleteIsolatedVertices);
			}
		//}

	}


	/**
	 * Removes a face from the mesh by removing all border edges of the face.
	 * If there is no border edge this method will not chage the mesh topology.
	 *
	 * Mesh changing method.
	 *
	 * Assumption: the face is at the border
	 *
	 * @param face                      the face that will be removed from the mesh
	 * @param deleteIsolatedVertices    true means that all vertices with degree <= 1 will be removed as well
	 */
	default void removeFaceAtBorder(@NotNull final F face, final boolean deleteIsolatedVertices) {
		if(!getMesh().isDestroyed(face)) {
			List<E> delEdges = new ArrayList<>();
            List<V> vertices = new ArrayList<>();

            // we only need the boundary if the face isNeighbourBorder
            F boundary = getMesh().getBorder();

            int count = 0;
            for(E edge : getMesh().getEdgeIt(face)) {
				F twinFace = getMesh().getTwinFace(edge);
                count++;
                if(twinFace.equals(boundary)) {
                    delEdges.add(edge);
                }
                else {
                    // update the edge of the boundary since it might be deleted!
                    getMesh().setEdge(boundary, edge);
                    getMesh().setFace(edge, boundary);
                }

                vertices.add(getMesh().getVertex(edge));
            }


            //TODO: this might be computational expensive!
            // special case: all edges will be deleted => adjust the border edge
            E borderEdge = null;
            if(getMesh().getTwinFace(getMesh().getEdge(boundary)) == face && delEdges.size() == count) {

                // all edges are border edges!
                borderEdge = getMesh().getTwin(getMesh().getEdge(face));
                EdgeIterator<P, V, E, F> edgeIterator = new EdgeIterator<>(getMesh(), borderEdge);

                // walk along the border away from this faces to get another edge which won't be deleted
                F twinFace = getMesh().getTwinFace(borderEdge);
                while (edgeIterator.hasNext() && twinFace == face) {
                    borderEdge = edgeIterator.next();
                    twinFace = getMesh().getTwinFace(borderEdge);
                }

                if(getMesh().getTwinFace(borderEdge) == face) {
                    borderEdge = getMesh().streamEdges().filter(e -> getMesh().getTwinFace(e) != face).filter(e -> getMesh().isBoundary(e)).findAny().get();
                    //throw new IllegalArgumentException("could not adjust border edge! Deletion of " + face + " is not allowed.");
                }

                getMesh().setFace(borderEdge, boundary);
                getMesh().setEdge(boundary, borderEdge);
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

                    // TODO: do we need this?
                    vertices.stream().filter(getMesh()::isAlive).forEach(v -> adjustVertex(v));
                }
            }
			if(count > 0) {
				getMesh().destroyFace(face);
			}
			else {
            	log.warn("could not delete face " + face + ". It is not at the border!");
			}

        }
	}

	/**
	 * Tests whether the vertex has degree smaller or equals 2.
	 * If an edge gets deleted and the vertex is simple connected
	 * the vertex becomes isolated.
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

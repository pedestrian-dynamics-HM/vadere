package org.vadere.util.geometry.mesh.inter;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Benedikt Zoennchen
 */
public interface IPolyConnectivity<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> extends Iterable<F>{

	IMesh<P, E, F> getMesh();

	default boolean isAtBoundary(E halfEdge) {
		IMesh<P, E, F> mesh = getMesh();
		return mesh.isBoundary(halfEdge) || mesh.isBoundary(mesh.getTwin(halfEdge));
	}

	default Optional<F> locate(final double x, final double y) {
		for(F face : getMesh().getFaces()) {
			VPolygon polygon = getMesh().toPolygon(face);
			if(polygon.contains(new VPoint(x, y))) {
				return Optional.of(face);
			}
		}
		return Optional.empty();
	}

	default Optional<F> locate(final IPoint point) {
		return locate(point.getX(), point.getY());
	}

	default boolean isAtBoundary(F face) {
		return getMesh().getEdges(face).stream().anyMatch(edge -> isAtBoundary(edge));
	}

	default void adjustVertex(P vertex){
		List<E> edges = getMesh().getEdges(vertex);
		edges.stream().filter(edge -> isAtBoundary(edge)).findAny().ifPresent(edge -> getMesh().setEdge(vertex, edge));
	}

	default Optional<E> findEdge(P begin, P end) {
		IMesh<P, E, F> mesh = getMesh();
		return mesh.getIncidentEdges(mesh.getEdge(begin)).stream().filter(edge -> mesh.getPrev(edge).equals(end)).map(edge -> mesh.getTwin(edge)).findAny();
	}

	default boolean isSimpleLink(E halfEdge) {
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

	default boolean isSimpleConnected(F face) {
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

	default void split(F face, P vertex) {
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
			E hnew = getMesh().createEdge(vertex);

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
	 * Removes a simple link.
	 *
	 * @param edge
	 * @return
	 */
	default F removeEdge(E edge) {
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

		/* adjust vertices, mb later
		P eVertex = getMesh().getVertex(edge);
		P tVertex = getMesh().getVertex(twin);
		*/

		if(getMesh().getEdge(remFace).equals(edge)) {
			getMesh().setEdge(remFace, prevTwin);
		}
		else if(getMesh().getEdge(remFace).equals(twin)) {
			getMesh().setEdge(remFace, prevEdge);
		}

		for(E halfEdge : getMesh().getEdgeIt(remFace)) {
			getMesh().setFace(halfEdge, remFace);
		}

		getMesh().destroyEdge(edge);
		getMesh().destroyEdge(twin);
		getMesh().destroyFace(delFace);

		return remFace;
	}

	default void removeFace(@NotNull F face, boolean deleteIsolatedVertices) {
		assert !getMesh().isDestroyed(face);

		getMesh().destroyFace(face);
		List<E> delEdges = new ArrayList<>();

		List<P> vertices = new ArrayList<>();

		F boundary = getMesh().createFace(true);

		for(E edge : getMesh().getEdgeIt(face)) {
			getMesh().setFace(edge, boundary);
			if(getMesh().isBoundary(getMesh().getTwin(edge))) {
				delEdges.add(edge);
			}

			vertices.add(getMesh().getVertex(edge));
		}

		if(!delEdges.isEmpty()) {
			E h0, h1, next0, next1, prev0, prev1;
			P v0, v1;

			for(E delEdge : delEdges) {
				h0 = delEdge;
				v0 = getMesh().getVertex(delEdge);
				next0 = getMesh().getNext(h0);
				prev0 = getMesh().getPrev(h0);

				h1    = getMesh().getTwin(delEdge);
				v1    = getMesh().getVertex(h1);
				next1 = getMesh().getNext(h1);
				prev1 = getMesh().getPrev(h1);

				// adjust next and prev handles
				getMesh().setNext(prev0, next1);
				getMesh().setNext(prev1, next0);

				// mark edge deleted if the mesh has a edge status
				getMesh().destroyEdge(h0);
				getMesh().destroyEdge(h1);

				// TODO: delete isolated vertices?


				for(P vertex : vertices) {
					adjustVertex(vertex);
				}
			}
		}
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

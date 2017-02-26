package org.vadere.util.geometry.mesh;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Benedikt Zoennchen
 */
public interface IPolyConnectivity<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> extends Iterable<F>{

	IMesh<P, E, F> getMesh();

	default boolean isBoundary(E halfEdge) {
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

	default Optional<F> locate(final P point) {
		return locate(point.getX(), point.getY());
	}

	default boolean isBoundary(F face) {
		return getMesh().getEdges(face).stream().anyMatch(edge -> isBoundary(edge));
	}

	default void adjustVertex(P vertex){
		List<E> edges = getMesh().getEdges(vertex);
		edges.stream().filter(edge -> isBoundary(edge)).findAny().ifPresent(edge -> getMesh().setEdge(vertex, edge));
	}

	default Optional<E> findEdge(P begin, P end) {
		IMesh<P, E, F> mesh = getMesh();
		return mesh.getNeighbours(mesh.getEdge(begin)).stream().filter(edge -> mesh.getPrev(edge).equals(end)).map(edge -> mesh.getTwin(edge)).findAny();
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

}

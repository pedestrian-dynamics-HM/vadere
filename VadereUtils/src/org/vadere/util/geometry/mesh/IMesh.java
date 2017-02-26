package org.vadere.util.geometry.mesh;

import org.apache.commons.collections.IteratorUtils;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.geom.Path2D;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * @author Benedikt Zoennchen
 * @param <P>
 */
public interface IMesh<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> extends Iterable<F> {
	E getNext(@NotNull E halfEdge);
	E getPrev(@NotNull E halfEdge);
	E getTwin(@NotNull E halfEdge);
	F getFace(@NotNull E halfEdge);

	E getEdge(@NotNull P vertex);
	E getEdge(@NotNull F face);

	P getVertex(@NotNull E halfEdge);

	default F getTwinFace(@NotNull E halfEdge) {
		return getFace(getTwin(halfEdge));
	}

	boolean isBoundary(@NotNull F face);
	boolean isBoundary(@NotNull E halfEdge);
	boolean isDestroyed(@NotNull F face);

	void setTwin(@NotNull E halfEdge, @NotNull E twin);
	void setNext(@NotNull E halfEdge, @NotNull E next);
	void setPrev(@NotNull E halfEdge, @NotNull E prev);
	void setFace(@NotNull E halfEdge, @NotNull F face);

	void setEdge(@NotNull F face, @NotNull E edge);
	void setEdge(@NotNull P vertex, @NotNull E edge);
	void setVertex(@NotNull E halfEdge, @NotNull P vertex);

	List<E> getEdges(@NotNull P vertex);

	default List<E> getEdges(@NotNull F face) {
		return IteratorUtils.toList(new EdgeIterator(this, face));
	}

	default List<F> getFaces(@NotNull E edge) { return IteratorUtils.toList(new NeighbourFaceIterator(this, edge)); }

	default List<E> getNeighbours(@NotNull E edge) { return IteratorUtils.toList(new NeighbourIterator(this, edge)); }

	default Iterable<E> getNeighbourIt(E edge) {
		return () -> new NeighbourIterator(this, edge);
	}

	default Iterable<E> getEdgeIt(F face) {
		return () -> new EdgeIterator(this, face);
	}

	default Iterable<F> getIncidentFacesIt(@NotNull E edge) { return () -> new NeighbourFaceIterator<>(this, edge); }

	E createEdge(@NotNull P vertex);
	E createEdge(@NotNull P vertex, @NotNull F face);
	F createFace();

	void destroyFace(@NotNull F face);
	void destroyEdge(@NotNull E edge);

	List<F> getFaces();

	@Override
	default Iterator<F> iterator() {
		return getFaces().iterator();
	}

	default VPolygon toPolygon(F face) {
		Path2D path2D = new Path2D.Double();
		E edge = getEdge(face);
		E prev = getPrev(edge);

		path2D.moveTo(getVertex(prev).getX(), getVertex(prev).getY());
		path2D.lineTo(getVertex(edge).getX(), getVertex(edge).getY());

		while (!edge.equals(prev)) {
			edge = getNext(edge);
			P p = getVertex(edge);
			path2D.lineTo(p.getX(), p.getY());
		}

		return new VPolygon(path2D);
	}

	default Optional<F> locate(final double x, final double y) {
		for(F face : getFaces()) {
			VPolygon polygon = toPolygon(face);
			if(polygon.contains(new VPoint(x, y))) {
				return Optional.of(face);
			}
		}
		return Optional.empty();
	}
}

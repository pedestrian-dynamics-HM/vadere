package org.vadere.util.geometry.data;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.geom.Path2D;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A Face is a region of a planar separation of the 2-D space, e.g. the region of a Polygon/Triangle and so on.
 *
 * @param <P> the type of the coordinates the face uses.
 */
public class Face<P extends IPoint> implements Iterable<HalfEdge<P>> {

	/**
	 * One of the half-edges bordering this face.
	 */
	private HalfEdge<P> edge;

	/**
	 * Default constructor. To construct a face where you have already some half-edges
	 * bordering this face.
	 *
	 * @param edge one of the half-edges bordering this face.
	 */
	public Face(@NotNull final HalfEdge<P> edge) {
		this.edge = edge;
	}

	/**
	 * This constructor can be used for constructing a new face without having
	 * constructed the bordering half-edges jet.
	 */
	public Face() {}

	public static <P extends IPoint> Face<P> of(P p1, P p2, P p3) {
		Face superTriangle = new Face();
		HalfEdge edge1 = new HalfEdge(p1, superTriangle);
		HalfEdge edge2 = new HalfEdge(p2, superTriangle);
		HalfEdge edge3 = new HalfEdge(p3, superTriangle);
		edge1.setNext(edge2);
		edge2.setNext(edge3);
		edge3.setNext(edge1);
		superTriangle.setEdge(edge1);
		return superTriangle;
	}

	/**
	 * Sets one of the half-edges bordering this face.
	 * @param edge half-edge bordering this face
	 */
	public void setEdge(@NotNull HalfEdge<P> edge) {
		this.edge = edge;
	}

	/**
	 * Computes the area of this face.
	 * @return the area of this face
	 */
	public double getArea() {
		return GeometryUtils.areaOfPolygon(getPoints());
	}

	/**
	 *
	 * @return
	 */
	public List<P> getPoints() {
		return streamPoints().collect(Collectors.toList());
	}

	public boolean contains(final P point) {
		return toPolygon().contains(point);
	}

	public VPolygon toPolygon() {
		Path2D path2D = new Path2D.Double();
		path2D.moveTo(edge.getPrevious().getEnd().getX(), edge.getPrevious().getEnd().getY());
		for(HalfEdge edge : this) {
			path2D.lineTo(edge.getEnd().getX(), edge.getEnd().getY());
		}
		return new VPolygon(path2D);
	}

	@Override
	public Iterator<HalfEdge<P>> iterator() {
		return new HalfEdgeIterator();
	}

	public Stream<HalfEdge<P>> stream () {
		Iterable<HalfEdge<P>> iterable = () -> iterator();
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	public List<HalfEdge<P>> getEdges() {
		return stream().collect(Collectors.toList());
	}

	public Stream<P> streamPoints() {
		return stream().map(edge -> edge.getEnd());
	}

	private class HalfEdgeIterator implements Iterator<HalfEdge<P>> {
		private HalfEdge<P> currentHalfEdge;
		private boolean started = false;

		private HalfEdgeIterator(){
			this.currentHalfEdge = edge;
		}

		@Override
		public boolean hasNext() {
			return currentHalfEdge != null && (!started || !currentHalfEdge.equals(edge));
		}

		@Override
		public HalfEdge<P> next() {
			started = true;
			HalfEdge result = currentHalfEdge;
			currentHalfEdge = currentHalfEdge.getNext();
			return result;
		}
	}
}

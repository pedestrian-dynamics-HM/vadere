package org.vadere.util.delaunay;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.data.DAG;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.geom.Path2D;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Face<P extends VPoint> implements Iterable<HalfEdge<P>> {

	/**
	 * one of the half-edges bordering this face.
	 */
	private HalfEdge<P> edge;

	public Face(final HalfEdge<P> edge) {
		this.edge = edge;
	}

	public Face() {}

	public static <P extends VPoint> Face<P> of(P p1, P p2, P p3) {
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

	public void setEdge(@NotNull HalfEdge<P> edge) {
		this.edge = edge;
	}

	public double getArea() {
		List<P> pointList = getPoints();

		double result = 0;
		for (int i = 0; i < pointList.size() - 1; i++) {
			result += (pointList.get(i).y + pointList.get(i + 1).y)
					* (pointList.get(i).x - pointList.get(i + 1).x);
		}
		return Math.abs(result) / 2.0;
	}

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

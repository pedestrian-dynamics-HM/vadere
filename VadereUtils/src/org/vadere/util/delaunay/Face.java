package org.vadere.util.delaunay;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Face implements Iterable<HalfEdge> {

	/**
	 * one of the half-edges bordering this face.
	 */
	private HalfEdge edge;

	public Face(final @NotNull HalfEdge edge) {
		this.edge = edge;
	}

	public double getArea() {
		List<VPoint> pointList = getPoints();

		double result = 0;
		for (int i = 0; i < pointList.size() - 1; i++) {
			result += (pointList.get(i).y + pointList.get(i + 1).y)
					* (pointList.get(i).x - pointList.get(i + 1).x);
		}
		return Math.abs(result) / 2.0;
	}

	public List<VPoint> getPoints() {
		List<VPoint> list = new ArrayList<>();
		for(HalfEdge edge : this) {
			list.add(edge.getEnd());
		}
		return list;
	}

	@Override
	public Iterator<HalfEdge> iterator() {
		return new HalfEdgeIterator();
	}


	private class HalfEdgeIterator implements Iterator<HalfEdge> {
		private HalfEdge currentHalfEdge;
		private boolean started = false;

		private HalfEdgeIterator(){
			this.currentHalfEdge = edge;
		}

		@Override
		public boolean hasNext() {
			return currentHalfEdge != null && (!started || !currentHalfEdge.equals(edge));
		}

		@Override
		public HalfEdge next() {
			started = true;
			HalfEdge result = currentHalfEdge;
			currentHalfEdge = currentHalfEdge.getNext();
			return result;
		}
	}
}

package org.vadere.util.geometry.data;

import org.apache.commons.collections.IteratorUtils;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HalfEdge<P extends IPoint> implements Iterable<HalfEdge<P>> {

	/**
	 * point at the end of the half edge.
	 */
	private P end;

	/**
	 * next half-edge around the face.
	 */
	private HalfEdge<P> next;

	/**
	 * previous half-edge around the face.
	 */
	private HalfEdge<P> previous;

	/**
	 * oppositely oriented adjacnet half-edge. If the face is a the boundary
	 * there is no twin.
	 */
	private HalfEdge<P> twin;

	/**
	 * the face the half-edge borders.
	 */
	private Face<P> face;


	public HalfEdge (@NotNull final P end, @NotNull final Face<P> face) {
		this.end = end;
		this.face = face;
	}

	public Face getFace() {
		return face;
	}

	public void setFace(final Face<P> face) {
		this.face = face;
	}

	public P getEnd() {
		return end;
	}

	public boolean hasNext() {
		return next != null;
	}

	public boolean hasTwin() {
		return twin != null;
	}

	public HalfEdge<P> getNext() {
		return next;
	}

	public HalfEdge<P> getPrevious() {
		return previous;
	}

	public HalfEdge<P> getTwin() {
		return twin;
	}

	public boolean isBoundary() {
		return face.isBorder();
	}

	/**
	 * removes the cyclic pointer structure such that the GC can deleteBoundaryFace these objects.
	 */
	public void destroy() {
		setNext(null);
		setPrevious(null);
		setTwin(null);
		setFace(null);
	}

	public boolean isValid() {
		return twin != null && next != null && previous != null && face != null;
	}

	public void setTwin(final HalfEdge twin) {
		this.twin = twin;
		if(twin != null && twin.getTwin() != this) {
			twin.setTwin(this);
		}
	}

	public void setPrevious(final HalfEdge<P> previous) {
		this.previous = previous;
		if(previous != null && previous.getNext() != this) {
			previous.setNext(this);
		}
	}

	public void setNext(final HalfEdge<P> next) {
		this.next = next;
		if(next != null && next.getPrevious() != this) {
			next.setPrevious(this);
		}
	}

	public void setEnd(P end) {
		this.end = end;
	}

	public VLine toLine() {
		return new VLine((VPoint) this.getPrevious().getEnd(), (VPoint) this.getEnd());
	}

	public Iterator<HalfEdge<P>> incidentVertexIterator() {
		return new NeighbourIterator();
	}

	public Iterator<Face<P>> incidentFaceIterator() { return new NeighbourFaceIterator(); }

	public List<Face<P>> getIncidentFaces() {
		return IteratorUtils.toList(incidentFaceIterator());
	}

	public List<HalfEdge<P>> getIncidentPoints() {
		List<HalfEdge<P>> incidentPoints = new ArrayList<>();
		Iterator<HalfEdge<P>> iterator = incidentVertexIterator();

		while (iterator.hasNext()) {
			incidentPoints.add(iterator.next());
		}

		return incidentPoints;
	}

	@Override
	public String toString() {
		return getEnd().toString();
	}

	@Override
	public Iterator<HalfEdge<P>> iterator() {
		return incidentVertexIterator();
	}

	/**
	 * This iterator assumes that the this edge is completely surrounded by faces.
	 */
	private class NeighbourFaceIterator implements Iterator<Face<P>> {
		private NeighbourIterator neighbourIterator = new NeighbourIterator();

		private NeighbourFaceIterator() {}

		@Override
		public boolean hasNext() {
			return neighbourIterator.hasNext();
		}

		@Override
		public Face<P> next() {
			return neighbourIterator.next().getFace();
		}
	}

	/**
	 * This iterator assumes that the this edge is completely surrounded by faces.
	 */
	private class NeighbourIterator implements Iterator<HalfEdge<P>> {

		private HalfEdge<P> current = HalfEdge.this.getNext();
		private boolean first = true;

		@Override
		public boolean hasNext() {
			return (first || current != HalfEdge.this.getNext());
		}

		@Override
		public HalfEdge<P> next() {
			HalfEdge<P> result = current;
			current = result.getTwin().getNext();
			first = false;
			return result;
		}
	}

	/*
	 * A half-edge is defined by its end vertex and its face. In a geometry there can not be more than
	 * one half-edge part of face and ending at end.
	 */

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		HalfEdge<?> halfEdge = (HalfEdge<?>) o;

		if (!end.equals(halfEdge.end)) return false;
		return face != null ? face.equals(halfEdge.face) : halfEdge.face == null;
	}

	@Override
	public int hashCode() {
		int result = end.hashCode();
		result = 31 * result + (face != null ? face.hashCode() : 0);
		return result;
	}
}

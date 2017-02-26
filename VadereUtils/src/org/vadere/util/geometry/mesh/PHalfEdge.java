package org.vadere.util.geometry.mesh;

import org.apache.commons.collections.IteratorUtils;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PHalfEdge<P extends IPoint> implements Iterable<PHalfEdge<P>>, IHalfEdge<P> {

	/**
	 * point at the end of the half edge.
	 */
	private P end;

	/**
	 * next half-edge around the face.
	 */
	private PHalfEdge<P> next;

	/**
	 * previous half-edge around the face.
	 */
	private PHalfEdge<P> previous;

	/**
	 * oppositely oriented adjacnet half-edge. If the face is a the boundary
	 * there is no twin.
	 */
	private PHalfEdge<P> twin;

	/**
	 * the face the half-edge borders.
	 */
	private Face<P> face;


	public PHalfEdge(@NotNull final P end, @NotNull final Face<P> face) {
		this.end = end;
		this.face = face;
	}

	public PHalfEdge(@NotNull final P end) {
		this.end = end;
		this.face = null;
	}

	public Face<P> getFace() {
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

	public PHalfEdge<P> getNext() {
		return next;
	}

	public PHalfEdge<P> getPrevious() {
		return previous;
	}

	public PHalfEdge<P> getTwin() {
		return twin;
	}

	public boolean hasSameTwinFace(final PHalfEdge<P> halfEdge) {
		return this.getTwin().getFace().equals(halfEdge.getTwin().getFace());
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

	public void setTwin(final PHalfEdge twin) {
		this.twin = twin;
		if(twin != null && twin.getTwin() != this) {
			twin.setTwin(this);
		}
	}

	public void setPrevious(final PHalfEdge<P> previous) {
		this.previous = previous;
		if(previous != null && previous.getNext() != this) {
			previous.setNext(this);
		}
	}

	public void setNext(final PHalfEdge<P> next) {
		this.next = next;
		if(next != null && next.getPrevious() != this) {
			next.setPrevious(this);
		}
	}

	public void setEnd(P end) {
		this.end = end;
	}

	public VLine toLine() {
		return new VLine(new VPoint(this.getPrevious().getEnd()), new VPoint(this.getEnd()));
	}

	public Iterator<PHalfEdge<P>> incidentVertexIterator() {
		return new NeighbourIterator();
	}

	public Iterator<Face<P>> incidentFaceIterator() { return new NeighbourFaceIterator(); }

	public List<Face<P>> getIncidentFaces() {
		return IteratorUtils.toList(incidentFaceIterator());
	}

	public List<PHalfEdge<P>> getIncidentPoints() {
		List<PHalfEdge<P>> incidentPoints = new ArrayList<>();
		Iterator<PHalfEdge<P>> iterator = incidentVertexIterator();

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
	public Iterator<PHalfEdge<P>> iterator() {
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
	private class NeighbourIterator implements Iterator<PHalfEdge<P>> {

		private PHalfEdge<P> current = PHalfEdge.this.getNext();
		private boolean first = true;

		@Override
		public boolean hasNext() {
			return (first || current != PHalfEdge.this.getNext());
		}

		@Override
		public PHalfEdge<P> next() {
			PHalfEdge<P> result = current;
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

		PHalfEdge<?> halfEdge = (PHalfEdge<?>) o;

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

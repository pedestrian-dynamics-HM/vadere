package org.vadere.util.geometry.data;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Iterator;
import java.util.Optional;

public class HalfEdge<P extends IPoint> {

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

	private double data;

	public HalfEdge (@NotNull final P end, @NotNull final Face<P> face) {
		this.end = end;
		this.face = face;
		this.data = 0.0;
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

	public void setTwin(final @NotNull HalfEdge twin) {
		this.twin = twin;
		if(twin.getTwin() != this) {
			twin.setTwin(this);
		}
	}

	public void setPrevious(final @NotNull HalfEdge<P> previous) {
		this.previous = previous;
		if(previous.getNext() != this) {
			previous.setNext(this);
		}
	}

	public void setNext(final @NotNull HalfEdge<P> next) {
		this.next = next;
		if(next.getPrevious() != this) {
			next.setPrevious(this);
		}
	}

	public VLine toLine() {
		return new VLine((VPoint) this.getPrevious().getEnd(), (VPoint) this.getEnd());
	}

	public double getData() {
		return data;
	}

	public void setData(double data) {
		this.data = data;
	}

	public Iterator<HalfEdge<P>> incidentPointIterator() {
		return new NeighbourIterator();
	}

	public Iterator<Face<P>> inciedentFaceIterator() { return new NeighbourFaceIterator(); }

	/**
	 * This iterator assumes that the this edge is completely surrounded by faces.
	 */
	private class NeighbourFaceIterator implements Iterator<Face<P>> {
		private NeighbourIterator neighbourIterator = new NeighbourIterator();

		private NeighbourFaceIterator() {
			// such that no duplicated faces returned
			if(neighbourIterator.hasNext()) {
				neighbourIterator.next();
			}
		}

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
			return result;
		}
	}

}

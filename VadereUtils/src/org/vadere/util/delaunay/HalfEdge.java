package org.vadere.util.delaunay;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Optional;

public class HalfEdge<P extends VPoint> {

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

	public Optional<HalfEdge<P>> getTwin() {
		return Optional.ofNullable(twin);
	}

	public void setTwin(final @NotNull HalfEdge twin) {
		this.twin = twin;
		if(!twin.getTwin().isPresent() || twin.getTwin().get() != this) {
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
		return new VLine(this.getPrevious().getEnd(), this.getEnd());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		HalfEdge halfEdge = (HalfEdge) o;

		if (!end.equals(halfEdge.end)) return false;
		if (next != null ? !next.equals(halfEdge.next) : halfEdge.next != null) return false;
		if (previous != null ? !previous.equals(halfEdge.previous) : halfEdge.previous != null)
			return false;
		if (twin != null ? !twin.equals(halfEdge.twin) : halfEdge.twin != null) return false;
		return face.equals(halfEdge.face);

	}

	@Override
	public int hashCode() {
		int result = end.hashCode();
		result = 31 * result + (next != null ? next.hashCode() : 0);
		result = 31 * result + (previous != null ? previous.hashCode() : 0);
		result = 31 * result + (twin != null ? twin.hashCode() : 0);
		result = 31 * result + face.hashCode();
		return result;
	}
}

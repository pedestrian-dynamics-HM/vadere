package org.vadere.util.delaunay;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * Created by bzoennchen on 13.11.16.
 */
public class HalfEdge {

	/**
	 * point at the end of the half edge.
	 */
	private VPoint end;

	/**
	 * next half-edge around the face.
	 */
	private HalfEdge next;

	/**
	 * previous half-edge around the face.
	 */
	private HalfEdge previous;

	/**
	 * oppositely oriented adjacnet half-edge. If the face is a the boundary
	 * there is no twin.
	 */
	private HalfEdge twin;

	/**
	 * the face the half-edge borders.
	 */
	private Face face;



	public HalfEdge (final VPoint end, final Face face) {
		this.end = end;
		this.face = face;
	}

	public VPoint getEnd() {
		return end;
	}

	public boolean hasNext() {
		return next != null;
	}

	public HalfEdge getNext() {
		return next;
	}

	public HalfEdge getPrevious() {
		return previous;
	}

	public HalfEdge getTwin() {
		return twin;
	}

	public void setTwin(final @NotNull HalfEdge twin) {
		this.twin = twin;
		if(twin.getTwin() != this) {
			twin.setTwin(this);
		}
	}

	public void setPrevious(final @NotNull HalfEdge previous) {
		this.previous = previous;
		if(previous.getNext() != this) {
			previous.setNext(this);
		}
	}

	public void setNext(final @NotNull HalfEdge next) {
		this.next = next;
		if(next.getPrevious() != this) {
			next.setPrevious(this);
		}
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

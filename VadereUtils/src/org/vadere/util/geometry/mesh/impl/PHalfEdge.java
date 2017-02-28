package org.vadere.util.geometry.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

public class PHalfEdge<P extends IPoint> implements IHalfEdge<P> {

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
	private PFace<P> face;


	PHalfEdge(@NotNull final P end, @NotNull final PFace<P> face) {
		this.end = end;
		this.face = face;
	}

	PHalfEdge(@NotNull final P end) {
		this.end = end;
		this.face = null;
	}

	PFace<P> getFace() {
		return face;
	}

	void setFace(final PFace<P> face) {
		this.face = face;
	}

	P getEnd() {
		return end;
	}

	boolean hasNext() {
		return next != null;
	}

	boolean hasTwin() {
		return twin != null;
	}

	PHalfEdge<P> getNext() {
		return next;
	}

	PHalfEdge<P> getPrevious() {
		return previous;
	}

	PHalfEdge<P> getTwin() {
		return twin;
	}

	boolean isBoundary() {
		return face.isBorder();
	}

	/**
	 * removes the cyclic pointer structure such that the GC can deleteBoundaryFace these objects.
	 */
	void destroy() {
		setNext(null);
		setPrevious(null);
		setTwin(null);
		setFace(null);
	}

	boolean isValid() {
		return twin != null && next != null && previous != null && face != null;
	}

	void setTwin(final PHalfEdge twin) {
		this.twin = twin;
		if(twin != null && twin.getTwin() != this) {
			twin.setTwin(this);
		}
	}

	void setPrevious(final PHalfEdge<P> previous) {
		this.previous = previous;
		if(previous != null && previous.getNext() != this) {
			previous.setNext(this);
		}
	}

	void setNext(final PHalfEdge<P> next) {
		this.next = next;
		if(next != null && next.getPrevious() != this) {
			next.setPrevious(this);
		}
	}

	void setEnd(P end) {
		this.end = end;
	}

	public VLine toLine() {
		return new VLine(new VPoint(this.getPrevious().getEnd()), new VPoint(this.getEnd()));
	}

	@Override
	public String toString() {
		return getEnd().toString();
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

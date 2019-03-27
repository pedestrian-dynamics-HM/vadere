package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

public class PHalfEdge<P extends IPoint, CE, CF> implements IHalfEdge<CE>, Cloneable {

	/**
	 * point at the end of the half edge.
	 */
	private PVertex<P, CE, CF> end;

	/**
	 * next half-edge around the face.
	 */
	private PHalfEdge<P, CE, CF> next;

	/**
	 * previous half-edge around the face.
	 */
	private PHalfEdge<P, CE, CF> previous;

	/**
	 * oppositely oriented adjacnet half-edge. If the face is a the boundary
	 * there is no twin.
	 */
	private PHalfEdge<P, CE, CF> twin;

	/**
	 * the face the half-edge borders.
	 */
	private PFace<P, CE, CF> face;

	private @Nullable CE data;

	private boolean destroyed;

	protected PHalfEdge(@NotNull final PVertex<P, CE, CF> end, @NotNull final PFace<P, CE, CF> face, @Nullable final CE data) {
		this.end = end;
		this.face = face;
		this.destroyed = false;
		this.data = data;
	}

	protected PHalfEdge(@NotNull final PVertex<P, CE, CF> end, @NotNull final PFace<P, CE, CF> face) {
		this(end, face, null);
	}

	protected PHalfEdge(@NotNull final PVertex<P, CE, CF> end) {
		this.end = end;
		this.face = null;
		this.destroyed = false;
		this.data = null;
	}

	PFace<P, CE, CF> getFace() {
		return face;
	}

	void setFace(final PFace<P, CE, CF> face) {
		this.face = face;
	}

	PVertex<P, CE, CF> getEnd() {
		return end;
	}

	boolean hasNext() {
		return next != null;
	}

	boolean hasTwin() {
		return twin != null;
	}

	PHalfEdge<P, CE, CF> getNext() {
		return next;
	}

	PHalfEdge<P, CE, CF> getPrevious() {
		return previous;
	}

	PHalfEdge<P, CE, CF> getTwin() {
		return twin;
	}

	boolean isBoundary() {
		return face.isBoundary();
	}

	/**
	 * removes the cyclic pointer structure such that the GC can deleteBoundaryFace these objects.
	 */
	void destroy() {
		setNext(null);
		setPrevious(null);
		setTwin(null);
		setFace(null);
		destroyed = true;
	}

	boolean isValid() {
		return twin != null && next != null && previous != null && face != null;
	}

	void setTwin(final PHalfEdge twin) {
		this.twin = twin;
	}

	void setPrevious(final PHalfEdge<P, CE, CF> previous) {
		this.previous = previous;
	}

	void setNext(final PHalfEdge<P, CE, CF> next) {
		this.next = next;
	}

	void setEnd(PVertex<P, CE, CF> end) {
		this.end = end;
	}

	void setData(@Nullable final CE data) {
		this.data = data;
	}

	@Nullable
	CE getData() {
		return data;
	}

	public VLine toLine() {
		return new VLine(new VPoint(this.getPrevious().getEnd()), new VPoint(this.getEnd()));
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public String toString() {
		if(destroyed) {
			return "destroyed half-edge";
		}
		return getEnd().toString();
	}

	@Override
	protected PHalfEdge<P, CE, CF> clone() throws CloneNotSupportedException {
		try {
			PHalfEdge<P, CE, CF> clone = (PHalfEdge<P, CE, CF>)super.clone();
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.getMessage());
		}
	}

	/*
	 * A half-edge is defined by its end vertex and its face. In a geometry there can not be more than
	 * one half-edge part of face and ending at end.
	 */

	/*@Override
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
	}*/
}

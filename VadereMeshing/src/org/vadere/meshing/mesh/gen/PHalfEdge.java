package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.HashMap;
import java.util.Map;

public class PHalfEdge implements IHalfEdge, Cloneable {

	private Map<String, Object> propertyElements;

	/**
	 * point at the end of the half edge.
	 */
	private PVertex end;

	/**
	 * next half-edge around the face.
	 */
	private PHalfEdge next;

	/**
	 * previous half-edge around the face.
	 */
	private PHalfEdge previous;

	/**
	 * oppositely oriented adjacnet half-edge. If the face is a the boundary
	 * there is no twin.
	 */
	private PHalfEdge twin;

	/**
	 * the face the half-edge borders.
	 */
	private PFace face;

	private boolean destroyed;

	protected PHalfEdge(@NotNull final PVertex end, @NotNull final PFace face) {
		this.end = end;
		this.face = face;
		this.destroyed = false;
		this.propertyElements = new HashMap<>();
	}

	protected PHalfEdge(@NotNull final PVertex end) {
		this.end = end;
		this.face = null;
		this.destroyed = false;
		this.propertyElements = new HashMap<>();
	}

	PFace getFace() {
		return face;
	}

	void setFace(final PFace face) {
		this.face = face;
	}

	PVertex getEnd() {
		return end;
	}

	boolean hasNext() {
		return next != null;
	}

	boolean hasTwin() {
		return twin != null;
	}

	PHalfEdge getNext() {
		return next;
	}

	PHalfEdge getPrevious() {
		return previous;
	}

	PHalfEdge getTwin() {
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

	void setPrevious(final PHalfEdge previous) {
		this.previous = previous;
	}

	void setNext(final PHalfEdge next) {
		this.next = next;
	}

	void setEnd(PVertex end) {
		this.end = end;
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
	protected PHalfEdge clone() throws CloneNotSupportedException {
		try {
			PHalfEdge clone = (PHalfEdge)super.clone();
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.getMessage());
		}
	}

	<T> void setData(final String name, T data) {
		propertyElements.put(name, data);
	}

	<T> T getData(final String name, Class<T> clazz) {
		if (propertyElements.containsKey(name)) {
			return clazz.cast(propertyElements.get(name));
		} else {
			return null;
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

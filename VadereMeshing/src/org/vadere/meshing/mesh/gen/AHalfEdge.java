package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * @author Benedikt Zoennchen
 */
public class AHalfEdge<P extends IPoint> implements IHalfEdge<P>, Cloneable {

	private int id;

	/**
	 * point at the end of the half edge.
	 */
	private int end;

	/**
	 * next half-edge around the face.
	 */
	private int next;

	/**
	 * previous half-edge around the face.
	 */
	private int previous;

	private int twin;

	/**
	 * the face the half-edge borders.
	 */
	private int face;

	private boolean destroyed;

	protected AHalfEdge(@NotNull final int id, @NotNull final int end, @NotNull final int face) {
		this.id = id;
	    this.end = end;
		this.face = face;
		this.destroyed = false;
	}

	protected AHalfEdge(@NotNull final int id, @NotNull final int end) {
		this.id = id;
	    this.end = end;
		this.face = -1;
		this.destroyed = false;
	}

	int getFace() {
		return face;
	}

	void setFace(final int face) {
		this.face = face;
	}

	int getEnd() {
		return end;
	}

	boolean hasNext() {
		return next != -1;
	}

	int getNext() {
		return next;
	}

	int getPrevious() {
		return previous;
	}

	int getId() {
		return id;
	}

	int getTwin() { return twin; }

	/**
	 * removes the cyclic pointer structure such that the GC can deleteBoundaryFace these objects.
	 */
	void destroy() {
		setNext(-1);
		setPrevious(-1);
		setFace(-1);
		destroyed = true;
	}

	boolean isValid() {
		return next != -1 && previous != -1 && face != -1;
	}


	void setPrevious(final int previous) {
		this.previous = previous;
	}

	void setNext(final int next) {
		this.next = next;
	}

	void setTwin(final int twin) {
	    this.twin = twin;
    }

	void setEnd(final int end) {
		this.end = end;
	}

	public boolean isDestroyed() {
		return destroyed;
	}

    /**
     * This method should only be called by the garbage collector in AMesh.
     * @param id
     */
    void setId(@NotNull final int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "" + id+"(e)->"+next+"(e), " + end+"(v)";
    }

    @Override
    protected AHalfEdge<P> clone() {
        try {
            return (AHalfEdge<P>)super.clone();
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

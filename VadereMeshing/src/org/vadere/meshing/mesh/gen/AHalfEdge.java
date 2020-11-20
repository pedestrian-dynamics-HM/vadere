package org.vadere.meshing.mesh.gen;

import org.vadere.meshing.mesh.inter.IHalfEdge;

/**
 * An array-based implementation of {@link IHalfEdge}.
 *
 * @author Benedikt Zoennchen
 */
public class AHalfEdge implements IHalfEdge, Cloneable {

	/**
	 * The array-index of this half-edge
	 */
	private int id;

	/**
	 * The array-index of the point at the end of the half edge.
	 */
	private int end;

	/**
	 * The array-index of the next half-edge around the face.
	 */
	private int next;

	/**
	 * The array-index of the previous half-edge around the face.
	 */
	private int previous;

	/**
	 * The array-index of the twin half-edge
	 */
	private int twin;

	/**
	 * The array-index of the face the half-edge borders.
	 */
	private int face;


	/**
	 * Indicates that the half-edge is destroyed and can be removed from the array-based data structure.
	 */
	private boolean destroyed;

	protected AHalfEdge(final int id, final int end, final int face) {
		this.id = id;
	    this.end = end;
		this.face = face;
		this.previous = -1;
		this.next = -1;
		this.twin = -1;
		this.destroyed = false;
	}

	protected AHalfEdge(final int id, final int end) {
		this.id = id;
	    this.end = end;
		this.face = -1;
		this.previous = -1;
		this.next = -1;
		this.twin = -1;
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

	boolean isDestroyed() {
		return destroyed;
	}

	/**
	 * Sets the array-index of this half-edge. Note that this method should
	 * only be called by the garbage collector in {@link AMesh} which
	 * adjust indices to remove destroyed base elements.
	 *
	 * @param id the new array-index of this face
	 */
    void setId(final int id) {
        this.id = id;
    }

	@Override
    public String toString() {
        return "" + id+"(e)->"+next+"(e), " + end+"(v)";
    }

    @Override
    protected AHalfEdge clone() {
        try {
            return (AHalfEdge)super.clone();
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
	}*/

	/*@Override
	public int hashCode() {
		super.hashCode()
		int result = end.hashCode();
		result = 31 * result + (face != null ? face.hashCode() : 0);
		return result;
	}*/
}

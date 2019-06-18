package org.vadere.meshing.mesh.gen;

import org.vadere.meshing.mesh.inter.IFace;

/**
 * An array-based implementation of {@link IFace}.
 *
 * @author Benedikt Zoennchen
 */
public class AFace implements IFace, Cloneable {

	/**
	 * One of the array index of the half-edges bordering this face.
	 */
	private int edge;

	/**
	 * The array-index of this face
	 */
	private int id;

	/**
	 * If true this face is a border face, otherwise its not.
	 */
	private boolean border;

	/**
	 * If true the face is destroyed which means it can be removed from the array-based mesh data structure.
	 */
	private boolean destroyed = false;

	/**
	 * Default constructor. To construct a face where you have already some half-edges
	 * bordering this face.
	 *  @param id     the array-index of this face
	 * @param edge   the array-index of one edge of this face
	 * @param border indicator if this face is a border face or not
	 */
	AFace(final int id, final int edge, boolean border) {
		this.border = border;
		this.edge = edge;
		this.id = id;
	}

	/**
	 * This constructor can be used for constructing a new face without having
	 * constructed the bordering half-edges jet.
	 */
	AFace(final int id, boolean border) {
		this.border = border;
		this.edge = -1;
		this.id = id;
	}

	/**
	 * Returns true if this face is a border face, false otherwise.
	 *
	 * @return true if this face is a border face, false otherwise
	 */
	boolean isBorder() {
		return border;
	}

	/**
	 * Destroys the face. After this call the face can be removed from the array based mesh data structure.
	 */
	void destroy() {
		setEdge(-1);
		destroyed = true;
	}

	/**
	 * Sets one of the half-edges bordering this face.
	 *
	 * @param edge the array-index of a half-edge bordering this face
	 */
	void setEdge(final int edge) {
		this.edge = edge;
	}

	/**
	 * Sets the array-index of this face. Note that this method should
	 * only be called by the garbage collector in {@link AMesh} which
	 * adjust indices to remove destroyed base elements.
	 *
	 * @param id the new array-index of this face
	 */
	void setId(final int id) {
		this.id = id;
	}

	/**
	 * Returns the array-index of an half-edge of this face.
	 *
	 * @return the array-index of an half-edge of this face
	 */
	int getEdge() {
		return edge;
	}

	/**
	 * Returns the array-indedx of this face.
	 *
	 * @return the array-indedx of this face.
	 */
	public int getId() {
	    return id;
    }

	/**
	 * Returns true if this face is destroyed and can be removed from the mesh data structure,
	 * false otherwise.
	 *
	 * @return true if this face is destroyed, false otherwise
	 */
	boolean isDestroyed() {
		return destroyed;
	}

	/**
	 * Sets this face to be a border face or a non-border face with respect to
	 * the parameter border.
	 *
	 * @param border if true this face becomes a border face, otherwise it becomes a non-border face
	 */
	public void setBorder(boolean border) {
		this.border = border;
	}

	@Override
    protected AFace clone()  {
        try {
            return (AFace) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

	@Override
	public String toString() {
		return id + "," + border +"," + destroyed;
	}
}

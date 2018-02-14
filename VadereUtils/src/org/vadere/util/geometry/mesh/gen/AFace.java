package org.vadere.util.geometry.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * @author Benedikt Zoennchen
 */
public class AFace<P extends IPoint> implements IFace<P>, Cloneable {

	/**
	 * One of the half-edges bordering this face.
	 */
	private int edge;

	private int id;

	private boolean border;

	private boolean destroyed = false;

	/**
	 * Default constructor. To construct a face where you have already some half-edges
	 * bordering this face.
	 *
	 * @param edge one of the half-edges bordering this face.
	 */
	AFace(@NotNull final int id, @NotNull final int edge) {
		this(id, edge, false);
	}

	AFace(@NotNull final int id,@NotNull final int edge, boolean border) {
		this.border = border;
		this.edge = edge;
		this.id = id;
	}

	/**
	 * This constructor can be used for constructing a new face without having
	 * constructed the bordering half-edges jet.
	 */
	AFace(@NotNull final int id, @NotNull boolean border) {
		this.border = border;
		this.edge = -1;
		this.id = id;
	}

	boolean isBorder() {
		return border;
	}

	void destroy() {
		setEdge(-1);
		destroyed = true;
	}

	/**
	 * Sets one of the half-edges bordering this face.
	 *
	 * @param edge half-edge bordering this face
	 */
	void setEdge(@NotNull final int edge) {
		this.edge = edge;
	}

	/**
	 * This method should only be called by the garbage collector in AMesh.
	 * @param id
	 */
	void setId(@NotNull final int id) {
		this.id = id;
	}

	int getEdge() {
		return edge;
	}

	public int getId() {
	    return id;
    }

	boolean isDestroyed() {
		return destroyed;
	}

	public void setBorder(boolean border) {
		this.border = border;
	}

	@Override
    protected AFace<P> clone()  {
        try {
            return (AFace<P>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }
}

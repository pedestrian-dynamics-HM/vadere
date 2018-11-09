package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * A Face is a region of a planar separation of the 2-D space, e.g. the region of a Polygon/Triangle and so on.
 *
 * @author Benedikt Zoennchen
 * @param <P> the type of the coordinates the face uses.
 */
public class PFace<P extends IPoint, CE, CF> implements IFace<CF>, Cloneable {

	/**
	 * One of the half-edges bordering this face.
	 */
	private PHalfEdge<P, CE, CF> edge;

	private boolean boundary;

	private boolean destroyed = false;

	/**
	 * Default constructor. To construct a face where you have already some half-edges
	 * bordering this face.
	 *
	 * @param edge one of the half-edges bordering this face.
	 */
	protected PFace(@NotNull final PHalfEdge<P, CE, CF> edge) {
		this(edge, false);
	}

	protected PFace(@NotNull final PHalfEdge<P, CE, CF> edge, boolean boundary) {
		this.boundary = boundary;
		this.edge = edge;
	}

	/**
	 * This constructor can be used for constructing a new face without having
	 * constructed the bordering half-edges jet.
	 */
	PFace(boolean boundary) {
		this.boundary = boundary;
	}

	PFace() {
		this.boundary = false;
	}

	boolean isBoundary() {
		return boundary;
	}

	void destroy() {
		setEdge(null);
		destroyed = true;
	}

	public void setBoundary(boolean border) {
		this.boundary = border;
	}

	/**
	 * Sets one of the half-edges bordering this face.
	 *
	 * @param edge half-edge bordering this face
	 */
	void setEdge(final PHalfEdge<P, CE, CF> edge) {
		this.edge = edge;
	}

	PHalfEdge<P, CE, CF> getEdge() {
		return edge;
	}

	boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public String toString() {
		PHalfEdge<P, CE, CF> current = edge;
		PHalfEdge<P, CE, CF> next = edge.getNext();
		StringBuilder builder = new StringBuilder();
		while (!edge.equals(next)) {
			builder.append(current + " ");
			current = next;
			next = current.getNext();
		}
		builder.append(current);
		return builder.toString();
	}

	/**
	 * Construct a deep clone / copy of this face!
	 * @return a deep clone of the face
	 * @throws CloneNotSupportedException if the method is not jet implemented.
	 */
	@Override
	protected PFace<P, CE, CF> clone() throws CloneNotSupportedException {
		try {
			PFace<P, CE, CF> clone = (PFace<P, CE, CF>)super.clone();
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.getMessage());
		}
	}
}

package org.vadere.util.geometry.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * A Face is a region of a planar separation of the 2-D space, e.g. the region of a Polygon/Triangle and so on.
 *
 * @author Benedikt Zoennchen
 * @param <P> the type of the coordinates the face uses.
 */
public class PFace<P extends IPoint> implements IFace<P> {

	/**
	 * One of the half-edges bordering this face.
	 */
	private PHalfEdge<P> edge;

	private boolean border;

	private boolean destroyed = false;

	/**
	 * Default constructor. To construct a face where you have already some half-edges
	 * bordering this face.
	 *
	 * @param edge one of the half-edges bordering this face.
	 */
	protected PFace(@NotNull final PHalfEdge<P> edge) {
		this(edge, false);
	}

	protected PFace(@NotNull final PHalfEdge<P> edge, boolean border) {
		this.border = border;
		this.edge = edge;
	}

	/**
	 * This constructor can be used for constructing a new face without having
	 * constructed the bordering half-edges jet.
	 */
	PFace(boolean border) {
		this.border = border;
	}

	PFace() {
		this.border = false;
	}

	boolean isBorder() {
		return border;
	}

	void destroy() {
		setEdge(null);
		destroyed = true;
	}

	/**
	 * Sets one of the half-edges bordering this face.
	 *
	 * @param edge half-edge bordering this face
	 */
	void setEdge(final PHalfEdge<P> edge) {
		this.edge = edge;
	}

	PHalfEdge<P> getEdge() {
		return edge;
	}

	boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public String toString() {
		PHalfEdge<P> current = edge;
		PHalfEdge<P> next = edge.getNext();
		StringBuilder builder = new StringBuilder();
		while (!edge.equals(next)) {
			builder.append(current + " ");
			current = next;
			next = current.getNext();
		}
		builder.append(current);
		return builder.toString();
	}
}

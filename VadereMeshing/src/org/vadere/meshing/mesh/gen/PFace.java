package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * A Face is a region of a planar separation of the 2-D space, e.g. the region of a Polygon/Triangle and so on.
 *
 * @author Benedikt Zoennchen
 * @param <P> the type of the coordinates the face uses.
 */
public class PFace<P extends IPoint, CE, CF> implements IFace<CF>, Cloneable {

	private static int MAX_FACE_PRINT_LEN = 100000;

	/**
	 * One of the half-edges bordering this face.
	 */
	private PHalfEdge<P, CE, CF> edge;

	private boolean boundary;

	private boolean destroyed = false;

	private @Nullable CF data;

	/**
	 * Default constructor. To construct a face where you have already some half-edges
	 * bordering this face.
	 *
	 * @param edge one of the half-edges bordering this face.
	 * @param boundary indicates if this edge is a boundary (border or hole) edge
	 * @param data the data associated and accessible via this face (possibly <tt>null</tt>)
	 */
	protected PFace(@NotNull final PHalfEdge<P, CE, CF> edge, final boolean boundary, @Nullable final CF data) {
		this.boundary = boundary;
		this.edge = edge;
		this.data = data;
	}

	/**
	 * The constructor to construct a face where you have already some half-edges
	 * bordering this face.
	 *
	 * @param edge one of the half-edges bordering this face.
	 */
	protected PFace(@NotNull final PHalfEdge<P, CE, CF> edge) {
		this(edge, false, null);
	}

	protected PFace(@NotNull final PHalfEdge<P, CE, CF> edge, boolean boundary) {
		this(edge, boundary, null);
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

	@Nullable
	CF getData() {
		return data;
	}

	void setData(@Nullable final CF data) {
		this.data = data;
	}

	@Override
	public String toString() {
		if(destroyed) {
			return "destroyed Face";
		}
		PHalfEdge<P, CE, CF> current = edge;
		PHalfEdge<P, CE, CF> next = edge.getNext();
		StringBuilder builder = new StringBuilder();
		int count = 0;
		while (count <= MAX_FACE_PRINT_LEN && !edge.equals(next)) {
			builder.append(current + " ");
			current = next;
			next = current.getNext();
			count++;
		}
		if(count > MAX_FACE_PRINT_LEN) {
			builder.insert(0, "LARGE-FACE:");
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

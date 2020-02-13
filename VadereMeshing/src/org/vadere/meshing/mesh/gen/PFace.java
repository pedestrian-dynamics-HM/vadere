package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;

import java.util.HashMap;
import java.util.Map;

/**
 * A Face is a region of a planar separation of the 2-D space, e.g. the region of a Polygon/Triangle and so on.
 *
 * @author Benedikt Zoennchen
 */
public class PFace implements IFace, Cloneable {

	private static int MAX_FACE_PRINT_LEN = 100000;

	private Map<String, Object> propertyElements;

	/**
	 * One of the half-edges bordering this face.
	 */
	private PHalfEdge edge;

	private boolean boundary;

	private boolean destroyed = false;

	/**
	 * Default constructor. To construct a face where you have already some half-edges
	 * bordering this face.
	 *  @param edge one of the half-edges bordering this face.
	 * @param boundary indicates if this edge is a boundary (border or hole) edge
	 */
	protected PFace(@NotNull final PHalfEdge edge, final boolean boundary) {
		this.boundary = boundary;
		this.edge = edge;
		this.propertyElements = new HashMap<>();
	}

	/**
	 * The constructor to construct a face where you have already some half-edges
	 * bordering this face.
	 *
	 * @param edge one of the half-edges bordering this face.
	 */
	protected PFace(@NotNull final PHalfEdge edge) {
		this(edge, false);
	}

	/**
	 * This constructor can be used for constructing a new face without having
	 * constructed the bordering half-edges jet.
	 */
	PFace(boolean boundary) {
		this.boundary = boundary;
		this.propertyElements = new HashMap<>();
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
		propertyElements.clear();
	}

	public void setBoundary(boolean border) {
		this.boundary = border;
	}

	/**
	 * Sets one of the half-edges bordering this face.
	 *
	 * @param edge half-edge bordering this face
	 */
	void setEdge(final PHalfEdge edge) {
		this.edge = edge;
	}

	PHalfEdge getEdge() {
		return edge;
	}

	boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public String toString() {
		if(destroyed) {
			return "destroyed Face";
		}
		PHalfEdge current = edge;
		PHalfEdge next = edge.getNext();
		StringBuilder builder = new StringBuilder();
		int count = 0;
		while (count <= MAX_FACE_PRINT_LEN && !edge.equals(next)) {
			builder.append(current + ",");
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
	protected PFace clone() throws CloneNotSupportedException {
		try {
			PFace clone = (PFace)super.clone();
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

}

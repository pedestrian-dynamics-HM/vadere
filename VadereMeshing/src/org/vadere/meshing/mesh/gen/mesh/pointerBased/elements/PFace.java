package org.vadere.meshing.mesh.gen.mesh.pointerBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;

import java.util.HashMap;
import java.util.Map;

/**
 * An pointer-based implementation of {@link IFace}.
 *
 * Original author: Benedikt Zoennchen
 * Refactored by: Hayato Hess
 */
public class PFace implements IFace, Cloneable {

	private static int MAX_FACE_PRINT_LEN = 100000;

	private Map<String, Object> propertyElements;

	/**
	 * One of the half-edges bordering this face.
	 */
	private PHalfEdge edge;

	private boolean isBoundary;

	private boolean destroyed = false;

	/**
	 * Default constructor. To construct a face where you have already some half-edges
	 * bordering this face.
	 *  @param edge one of the half-edges bordering this face.
	 * @param isBoundary indicates if this edge is a boundary (border or hole) edge
	 */
	protected PFace(@NotNull final PHalfEdge edge, final boolean isBoundary) {
		this.isBoundary = isBoundary;
		this.edge = edge;
		this.propertyElements = new HashMap<>();
	}

	/**
	 * Copy Constructor
	 */
	public PFace(PFace toCopy) {
		this.isBoundary = toCopy.isBoundary;
		this.edge = null; // set later
		this.propertyElements = new HashMap<>(toCopy.propertyElements);
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
	PFace(boolean isBoundary) {
		this.isBoundary = isBoundary;
		this.propertyElements = new HashMap<>();
	}

	PFace() {
		this.isBoundary = false;
	}

	boolean isBoundary() {
		return isBoundary;
	}

	void destroy() {
		setEdge(null);
		destroyed = true;
		propertyElements.clear();
	}

	public void setBoundary(boolean border) {
		this.isBoundary = border;
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

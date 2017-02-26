package org.vadere.util.geometry.mesh;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.Iterator;

/**
 * This iterator assumes that the this edge is completely surrounded by faces.
 *
 * @author Benedikt Zoennchen
 */
public
class NeighbourFaceIterator<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> implements Iterator<F> {
	private NeighbourIterator<P, E, F> neighbourIterator;
	private IMesh<P, E, F> mesh;

	public NeighbourFaceIterator(final IMesh<P, E, F> mesh, E edge) {
		this.neighbourIterator = new NeighbourIterator<>(mesh, edge);
		this.mesh = mesh;
	}

	@Override
	public boolean hasNext() {
		return neighbourIterator.hasNext();
	}

	@Override
	public F next() {
		return mesh.getFace(neighbourIterator.next());
	}
}
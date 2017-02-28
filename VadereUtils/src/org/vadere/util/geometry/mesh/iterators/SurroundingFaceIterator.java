package org.vadere.util.geometry.mesh.iterators;

import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Iterator;

/**
 * This Iterator iterates over all twin faces of its half-edges.
 *
 * @author Benedikt Zoennchen
 * @param <P> the type of the vertex
 * @param <E> the type of the half-edge
 * @param <F> the type of the face
 */
public class SurroundingFaceIterator<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> implements Iterator<F> {

	private EdgeIterator<P, E, F> edgeIterator;
	private IMesh<P, E, F> mesh;

	public SurroundingFaceIterator(final IMesh<P, E, F> mesh, final F face) {
		this.mesh = mesh;
		this.edgeIterator = new EdgeIterator<>(mesh, face);
	}

	@Override
	public boolean hasNext() {
		return edgeIterator.hasNext();
	}

	@Override
	public F next() {
		return mesh.getTwinFace(edgeIterator.next());
	}
}

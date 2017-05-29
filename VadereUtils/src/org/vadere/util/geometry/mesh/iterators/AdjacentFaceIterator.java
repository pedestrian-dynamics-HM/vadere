package org.vadere.util.geometry.mesh.iterators;

import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Iterator;

/**
 * This iterator assumes that this edge is completely surrounded by faces.
 * It iterates over all faces which are adjacent to the vertex of the edge
 * of this iterator.
 *
 * @author Benedikt Zoennchen
 * @param <P> the type of the vertex
 * @param <E> the type of the half-edge
 * @param <F> the type of the face
 */
public class AdjacentFaceIterator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements Iterator<F> {
	private IncidentEdgeIterator<P, V, E, F> neighbourIterator;
	private IMesh<P, V, E, F> mesh;

	public AdjacentFaceIterator(final IMesh<P, V, E, F> mesh, E edge) {
		this.neighbourIterator = new IncidentEdgeIterator<>(mesh, edge);
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
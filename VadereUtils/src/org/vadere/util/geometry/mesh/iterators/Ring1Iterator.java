package org.vadere.util.geometry.mesh.iterators;

import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Iterator;

/**
 * This iterator assumes that the mesh is a triangulation.
 * The iterator returns all edges which are surrounding in the perspective of the vertex of the edge.
 * If the vertex is part of the boundary/border duplicates might be returned!
 *
 * @author Benedikt Zoennchen
 * @param <P> the type of the vertex
 * @param <E> the type of the half-edge
 * @param <F> the type of the face
 */
public class Ring1Iterator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements Iterator<E> {

	private IncidentEdgeIterator<P, V, E, F> neighbourIterator;
	private IMesh<P, V, E, F> mesh;

	public Ring1Iterator(final IMesh<P, V, E, F> mesh, final E edge) {
		this.neighbourIterator = new IncidentEdgeIterator<>(mesh, edge);
		this.mesh = mesh;
	}

	@Override
	public boolean hasNext() {
		return neighbourIterator.hasNext();
	}

	@Override
	public E next() {
		E next = neighbourIterator.next();
		if(mesh.isBoundary(next)) {
			return mesh.getPrev(mesh.getTwin(next));
		}
		else {
			return mesh.getNext(next);
		}
	}
}

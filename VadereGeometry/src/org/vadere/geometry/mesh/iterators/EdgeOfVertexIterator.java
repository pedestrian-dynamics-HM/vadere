package org.vadere.geometry.mesh.iterators;

import org.vadere.geometry.mesh.inter.IFace;
import org.vadere.geometry.mesh.inter.IHalfEdge;
import org.vadere.geometry.mesh.inter.IMesh;
import org.vadere.geometry.mesh.inter.IVertex;
import org.vadere.geometry.shapes.IPoint;

import java.util.Iterator;

/**
 * @author Benedikt Zoennchen
 *
 * This iterator iterates over all half-edges which end point is equal to a specific vertex.
 *
 * @param <P> the type of the vertex
 * @param <E> the type of the half-edge
 * @param <F> the type of the face
 */
public class EdgeOfVertexIterator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements Iterator<E> {

	private IncidentEdgeIterator<P, V, E, F> edgeIterator;
	private final IMesh<P, V, E, F> mesh;

	public EdgeOfVertexIterator(final IMesh<P, V, E, F> mesh, final V vertex){
		this.edgeIterator = new IncidentEdgeIterator<>(mesh, mesh.getEdge(vertex));
		this.mesh = mesh;
	}

 	@Override
	public boolean hasNext() {
		return edgeIterator.hasNext();
	}

	@Override
	public E next() {
		return mesh.getTwin(edgeIterator.next());
	}
}

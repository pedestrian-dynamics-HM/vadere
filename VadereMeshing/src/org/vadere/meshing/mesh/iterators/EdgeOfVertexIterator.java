package org.vadere.meshing.mesh.iterators;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Iterator;

/**
 * @author Benedikt Zoennchen
 *
 * This iterator iterates over all half-edges which end point is equal to a specific vertex.
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class EdgeOfVertexIterator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Iterator<E> {

	private IncidentEdgeIterator<V, E, F> edgeIterator;
	private final IMesh<V, E, F> mesh;

	public EdgeOfVertexIterator(final IMesh<V, E, F> mesh, final V vertex){
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

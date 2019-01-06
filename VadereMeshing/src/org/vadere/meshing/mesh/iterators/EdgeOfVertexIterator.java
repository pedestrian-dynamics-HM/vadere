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
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class EdgeOfVertexIterator<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements Iterator<E> {

	private IncidentEdgeIterator<P, CE, CF, V, E, F> edgeIterator;
	private final IMesh<P, CE, CF, V, E, F> mesh;

	public EdgeOfVertexIterator(final IMesh<P, CE, CF, V, E, F> mesh, final V vertex){
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

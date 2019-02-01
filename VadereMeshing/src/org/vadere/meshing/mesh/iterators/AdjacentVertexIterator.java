package org.vadere.meshing.mesh.iterators;


import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.logging.Logger;

import java.util.Iterator;

/**
 * This iterator iterates over the adjacent vertices of the vertex of this iterator.
 *
 * @author Benedikt Zoennchen
 * @param <P> the type of the vertex
 * @param <E> the type of the half-edge
 * @param <F> the type of the face
 */
public class AdjacentVertexIterator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements Iterator<V> {

	private static Logger log = Logger.getLogger(AdjacentVertexIterator.class);
	private IMesh<P, V, E, F> mesh;
	private IncidentEdgeIterator<P, V, E, F> incidentEdgeIterator;

	public AdjacentVertexIterator(final IMesh<P, V, E, F> mesh, final V vertex) {
		this.mesh = mesh;
		this.incidentEdgeIterator = new IncidentEdgeIterator<>(mesh, mesh.getEdge(vertex));
	}

	@Override
	public boolean hasNext() {
		return incidentEdgeIterator.hasNext();
	}

	@Override
	public V next() {
		return mesh.getVertex(incidentEdgeIterator.next());
	}
}


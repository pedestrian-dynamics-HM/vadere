package org.vadere.geometry.mesh.iterators;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.geometry.mesh.inter.IFace;
import org.vadere.geometry.mesh.inter.IHalfEdge;
import org.vadere.geometry.mesh.inter.IMesh;
import org.vadere.geometry.mesh.inter.IVertex;
import org.vadere.geometry.shapes.IPoint;

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

	private static Logger log = LogManager.getLogger(AdjacentVertexIterator.class);
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


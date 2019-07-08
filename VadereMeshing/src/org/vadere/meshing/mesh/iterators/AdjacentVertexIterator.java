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
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class AdjacentVertexIterator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Iterator<V> {

	private static Logger log = Logger.getLogger(AdjacentVertexIterator.class);
	private IMesh<V, E, F> mesh;
	private IncidentEdgeIterator<V, E, F> incidentEdgeIterator;

	public AdjacentVertexIterator(final IMesh<V, E, F> mesh, final V vertex) {
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


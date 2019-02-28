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
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class AdjacentVertexIterator<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements Iterator<V> {

	private static Logger log = Logger.getLogger(AdjacentVertexIterator.class);
	private IMesh<P, CE, CF, V, E, F> mesh;
	private IncidentEdgeIterator<P, CE, CF, V, E, F> incidentEdgeIterator;

	public AdjacentVertexIterator(final IMesh<P, CE, CF, V, E, F> mesh, final V vertex) {
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


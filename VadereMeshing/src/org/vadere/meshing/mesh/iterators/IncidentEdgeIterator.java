package org.vadere.meshing.mesh.iterators;

import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IVertex;
import org.vadere.util.logging.Logger;

import java.util.Iterator;

/**
 * This iterator iterates over the adjacent edges of the vertex of the edge of this iterator.
 * Each adjacent edge contains an adjacent vertex with respect to the vertex of the edge if this
 * iterator.
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class IncidentEdgeIterator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Iterator<E> {

	private static Logger log = Logger.getLogger(IncidentEdgeIterator.class);
	private IMesh<V, E, F> mesh;
	private E current;
	private E edge;
	private boolean first;
	int count = 0;

	public IncidentEdgeIterator(final IMesh<V, E, F> mesh, final V vertex) {
		this(mesh, mesh.edges().getOf(vertex));
	}

	public IncidentEdgeIterator(final IMesh<V, E, F> mesh, final E edge) {
		this.mesh = mesh;
		this.edge = edge;
		this.current = mesh.edges().getTwin(edge);
		this.first = true;
	}

	@Override
	public boolean hasNext() {
		return (first || current != mesh.edges().getTwin(edge));
	}

	@Override
	public E next() {
		E result = current;
		current = mesh.edges().getTwin(mesh.edges().getPrev(result));
		first = false;
		count++;
		//log.info(count);

		return result;
	}
}

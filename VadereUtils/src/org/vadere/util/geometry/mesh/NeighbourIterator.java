package org.vadere.util.geometry.mesh;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.Iterator;

/**
 * @author Benedikt Zoennchen
 * @param <P>
 * @param <E>
 * @param <F>
 */
public class NeighbourIterator<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> implements Iterator<E> {

	private IMesh<P, E, F> mesh;
	private E current;
	private E edge;
	private boolean first;

	public NeighbourIterator(final IMesh<P, E, F> mesh, final E edge) {
		this.mesh = mesh;
		this.edge = edge;
		this.current = mesh.getNext(edge);
		this.first = true;
	}

	@Override
	public boolean hasNext() {
		return (first || current != mesh.getNext(edge));
	}

	@Override
	public E next() {
		E result = current;
		current = mesh.getNext(mesh.getTwin(result));
		first = false;
		return result;
	}
}

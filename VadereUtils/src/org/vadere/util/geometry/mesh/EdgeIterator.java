package org.vadere.util.geometry.mesh;

import org.vadere.util.geometry.shapes.IPoint;
import java.util.Iterator;


/**
 * @author Benedikt Zoennchen
 */
public class EdgeIterator<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> implements Iterator<E> {
	private E currentHalfEdge;
	private E edge;
	private boolean started = false;
	private IMesh<P, E, F> mesh;

	public EdgeIterator(final IMesh<P, E, F> mesh, final F face){
		this.edge = mesh.getEdge(face);
		this.currentHalfEdge = edge;
		this.mesh = mesh;
	}

	@Override
	public boolean hasNext() {
		return currentHalfEdge != null && (!started || !currentHalfEdge.equals(edge));
	}

	@Override
	public E next() {
		started = true;
		E result = currentHalfEdge;
		currentHalfEdge = mesh.getNext(currentHalfEdge);
		return result;
	}
}
package org.vadere.geometry.mesh.iterators;

import org.vadere.geometry.mesh.inter.IFace;
import org.vadere.geometry.mesh.inter.IHalfEdge;
import org.vadere.geometry.mesh.inter.IMesh;
import org.vadere.geometry.mesh.inter.IVertex;
import org.vadere.geometry.shapes.IPoint;
import java.util.Iterator;


/**
 * This Iterator iterates over all half-edges of a specific face.
 *
 * @author Benedikt Zoennchen
 * @param <P> the type of the vertex
 * @param <E> the type of the half-edge
 * @param <F> the type of the face
 */
public class EdgeIterator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements Iterator<E> {
	private E currentHalfEdge;
	private E edge;
	private boolean started = false;
	private IMesh<P, V, E, F> mesh;

	public EdgeIterator(final IMesh<P, V, E, F> mesh, final F face){
		this.edge = mesh.getEdge(face);
		this.currentHalfEdge = edge;
		this.mesh = mesh;
	}

	public EdgeIterator(final IMesh<P, V, E, F> mesh, final E edge){
		this.edge = edge;
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
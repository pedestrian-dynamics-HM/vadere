package org.vadere.meshing.mesh.iterators;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import java.util.Iterator;


/**
 * This Iterator iterates over all half-edges of a specific face.
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class EdgeIterator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Iterator<E> {
	private E currentHalfEdge;
	private E edge;
	private boolean started = false;
	private boolean reverse = false;
	private IMesh<V, E, F> mesh;

	public EdgeIterator(final IMesh<V, E, F> mesh, final F face){
		this.edge = mesh.getEdge(face);
		this.currentHalfEdge = edge;
		this.mesh = mesh;
	}

	public EdgeIterator(final IMesh<V, E, F> mesh, final E edge){
		this.edge = edge;
		this.currentHalfEdge = edge;
		this.mesh = mesh;
	}

	public EdgeIterator(final IMesh<V, E, F> mesh, final E edge, final boolean reverse){
		this.edge = edge;
		this.currentHalfEdge = edge;
		this.mesh = mesh;
		this.reverse = reverse;
	}


	@Override
	public boolean hasNext() {
		return currentHalfEdge != null && (!started || !currentHalfEdge.equals(edge));
	}

	@Override
	public E next() {
		started = true;
		E result = currentHalfEdge;
		currentHalfEdge = reverse ? mesh.getPrev(currentHalfEdge) : mesh.getNext(currentHalfEdge);
		return result;
	}
}
package org.vadere.meshing.mesh.iterators;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;

import java.util.Iterator;

public class EdgeIteratorReverse<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Iterator<E> {
	private E currentHalfEdge;
	private E edge;
	private boolean started = false;
	private IMesh<V, E, F> mesh;

	public EdgeIteratorReverse(final IMesh<V, E, F> mesh, final F face){
		this.edge = mesh.getEdge(face);
		this.currentHalfEdge = edge;
		this.mesh = mesh;
	}

	public EdgeIteratorReverse(final IMesh<V, E, F> mesh, final E edge){
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
		currentHalfEdge = mesh.getPrev(currentHalfEdge);
		return result;
	}
}
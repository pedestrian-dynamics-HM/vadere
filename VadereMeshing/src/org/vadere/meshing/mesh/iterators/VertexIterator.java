package org.vadere.meshing.mesh.iterators;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Iterator;

/**
 * This Iterator iterates over all vertices of a specific face.
 *
 * @author Benedikt Zoennchen
 * @param <P> the type of the vertex
 * @param <E> the type of the half-edge
 * @param <F> the type of the face
 */
public class VertexIterator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements Iterator<V> {
	private EdgeIterator<P, V, E, F> edgeIterator;
	private IMesh<P, V, E, F> mesh;

	public VertexIterator(final IMesh<P, V, E, F> mesh, final F face){
		this.mesh = mesh;
		this.edgeIterator = new EdgeIterator<>(mesh, face);
	}

	@Override
	public boolean hasNext() {
		return edgeIterator.hasNext();
	}

	@Override
	public V next() {
		return mesh.getVertex(edgeIterator.next());
	}
}

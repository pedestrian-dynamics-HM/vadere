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
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class VertexIterator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Iterator<V> {
	private EdgeIterator<V, E, F> edgeIterator;
	private IMesh<V, E, F> mesh;

	public VertexIterator(final IMesh<V, E, F> mesh, final F face){
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

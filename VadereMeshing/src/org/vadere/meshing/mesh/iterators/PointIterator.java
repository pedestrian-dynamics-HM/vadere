package org.vadere.meshing.mesh.iterators;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Iterator;

/**
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class PointIterator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Iterator<IPoint> {
	private VertexIterator<V, E, F> vertexIterator;
	private IMesh<V, E, F> mesh;

	public PointIterator(final IMesh<V, E, F> mesh, final F face){
		this.mesh = mesh;
		this.vertexIterator = new VertexIterator<>(mesh, face);
	}

	@Override
	public boolean hasNext() {
		return vertexIterator.hasNext();
	}

	@Override
	public IPoint next() {
		return mesh.getPoint(vertexIterator.next());
	}
}


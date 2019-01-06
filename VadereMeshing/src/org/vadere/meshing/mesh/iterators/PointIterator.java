package org.vadere.meshing.mesh.iterators;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Iterator;

/**
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class PointIterator<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements Iterator<P> {
	private VertexIterator<P, CE, CF, V, E, F> vertexIterator;
	private IMesh<P, CE, CF, V, E, F> mesh;

	public PointIterator(final IMesh<P, CE, CF, V, E, F> mesh, final F face){
		this.mesh = mesh;
		this.vertexIterator = new VertexIterator<>(mesh, face);
	}

	@Override
	public boolean hasNext() {
		return vertexIterator.hasNext();
	}

	@Override
	public P next() {
		return mesh.getPoint(vertexIterator.next());
	}
}


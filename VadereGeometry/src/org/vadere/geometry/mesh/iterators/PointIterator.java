package org.vadere.geometry.mesh.iterators;

import org.vadere.geometry.mesh.inter.IFace;
import org.vadere.geometry.mesh.inter.IHalfEdge;
import org.vadere.geometry.mesh.inter.IMesh;
import org.vadere.geometry.mesh.inter.IVertex;
import org.vadere.geometry.shapes.IPoint;

import java.util.Iterator;

public class PointIterator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements Iterator<P> {
	private VertexIterator<P, V, E, F> vertexIterator;
	private IMesh<P, V, E, F> mesh;

	public PointIterator(final IMesh<P, V, E, F> mesh, final F face){
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


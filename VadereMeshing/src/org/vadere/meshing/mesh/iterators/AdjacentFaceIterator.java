package org.vadere.meshing.mesh.iterators;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Iterator;

/**
 * This iterator assumes that the edge is completely surrounded by faces.
 * It iterates over all faces which are adjacent to the vertex of the edge
 * of this iterator.
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class AdjacentFaceIterator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Iterator<F> {
	private IncidentEdgeIterator<V, E, F> neighbourIterator;
	private IMesh<V, E, F> mesh;

	public AdjacentFaceIterator(final IMesh<V, E, F> mesh, E edge) {
		this.neighbourIterator = new IncidentEdgeIterator<>(mesh, edge);
		this.mesh = mesh;
	}

	@Override
	public boolean hasNext() {
		return neighbourIterator.hasNext();
	}

	@Override
	public F next() {
		return mesh.getFace(neighbourIterator.next());
	}
}
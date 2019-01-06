package org.vadere.meshing.mesh.iterators;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Iterator;

/**
 * This Iterator iterates over all twin faces of its half-edges.
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class SurroundingFaceIterator<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements Iterator<F> {

	private EdgeIterator<P, CE, CF, V, E, F> edgeIterator;
	private IMesh<P, CE, CF, V, E, F> mesh;

	public SurroundingFaceIterator(@NotNull final IMesh<P, CE, CF, V, E, F> mesh, @NotNull final F face) {
		assert mesh.isAlive(face);
		this.mesh = mesh;
		this.edgeIterator = new EdgeIterator<>(mesh, face);
	}

	@Override
	public boolean hasNext() {
		return edgeIterator.hasNext();
	}

	@Override
	public F next() {
		return mesh.getTwinFace(edgeIterator.next());
	}
}

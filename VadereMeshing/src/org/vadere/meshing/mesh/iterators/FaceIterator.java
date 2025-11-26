package org.vadere.meshing.mesh.iterators;

import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IVertex;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class FaceIterator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Iterator<F> {

	private LinkedList<F> facesToVisit;
	private Set<F> visitedFaces;
	private Predicate<F> facePredicate;
	private IMesh<V, E, F> mesh;

	public FaceIterator(final IMesh<V, E, F> mesh, final Predicate<F> facePredicate) {
		this.mesh = mesh;
		this.facesToVisit = new LinkedList<>();
		F face = mesh.faces().getFirst();
		F startFace = mesh.faces().isBoundary(face) ? mesh.faces().getTwin(mesh.edges().getAnyOf(face)) : face;

		if(mesh.faces().isDestroyed(startFace)) {
			throw new IllegalArgumentException("this face is already destroyed.");
		}

		facesToVisit.add(startFace);
		visitedFaces = new HashSet<>();
		this.facePredicate = facePredicate;
	}

	public FaceIterator(final IMesh<V, E, F> mesh) {
		this(mesh, f -> true);
	}

	@Override
	public boolean hasNext() {
		return !facesToVisit.isEmpty();
	}

	@Override
	public F next() {
		F nextFace = facesToVisit.removeFirst();
		visitedFaces.add(nextFace);

		for(E he : mesh.edges().iterableFor(nextFace)) {
			F twinFace = mesh.faces().getTwin(he);

 			if(mesh.faces().isBoundary(twinFace) || mesh.faces().isDestroyed(twinFace) || !facePredicate.test(twinFace)) {
				visitedFaces.add(twinFace);
			}

			if(!visitedFaces.contains(twinFace)) {
				facesToVisit.add(twinFace);
			}

			visitedFaces.add(twinFace);
		}

		return nextFace;
	}
}

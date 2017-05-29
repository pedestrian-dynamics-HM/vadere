package org.vadere.util.geometry.mesh.iterators;

import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author Benedikt Zoennchen
 *
 * @param <P>
 */
public class FaceIterator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements Iterator<F> {

	private LinkedList<F> facesToVisit;
	private Set<F> visitedFaces;
	private Predicate<F> facePredicate;
	private IMesh<P, V, E, F> mesh;

	public FaceIterator(final IMesh<P, V, E, F> mesh, final Predicate<F> facePredicate) {
		this.mesh = mesh;
		this.facesToVisit = new LinkedList<>();
		F face = mesh.getFace();
		F startFace = mesh.isBoundary(face) ? mesh.getTwinFace(mesh.getEdge(face)) : face;

		if(mesh.isDestroyed(startFace)) {
			throw new IllegalArgumentException("this face is already destroyed.");
		}

		facesToVisit.add(startFace);
		visitedFaces = new HashSet<>();
		this.facePredicate = facePredicate;
	}

	public FaceIterator(final IMesh<P, V, E, F> mesh) {
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

		for(E he : mesh.getEdgeIt(nextFace)) {
			F twinFace = mesh.getTwinFace(he);

 			if(mesh.isBoundary(twinFace) || mesh.isDestroyed(twinFace) || !facePredicate.test(twinFace)) {
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

package org.vadere.util.geometry.data;

import org.vadere.util.geometry.data.Face;
import org.vadere.util.geometry.data.HalfEdge;
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
public class FaceIterator<P extends IPoint> implements Iterator<Face<P>> {

	private LinkedList<Face<P>> facesToVisit;
	private Set<Face<P>> visitedFaces;
	private Predicate<Face<P>> facePredicate;

	public FaceIterator(final Face<P> face, final Predicate<Face<P>> facePredicate) {
		facesToVisit = new LinkedList<>();
		Face<P> startFace = face.isBorder() ? face.getEdge().getTwin().getFace() : face;

		if(startFace.isDestroyed()) {
			throw new IllegalArgumentException("this face is already destroyed.");
		}

		facesToVisit.add(startFace);
		visitedFaces = new HashSet<>();
		this.facePredicate = facePredicate;
	}

	public FaceIterator(final Face<P> face) {
		this(face, f -> true);
	}

	@Override
	public boolean hasNext() {
		return !facesToVisit.isEmpty();
	}

	@Override
	public Face<P> next() {
		Face<P> nextFace = facesToVisit.removeFirst();
		visitedFaces.add(nextFace);

		for(HalfEdge<P> he : nextFace) {
			Face<P> twinFace = he.getTwin().getFace();

 			if(twinFace.isBorder() || twinFace.isDestroyed() || !facePredicate.test(twinFace)) {
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

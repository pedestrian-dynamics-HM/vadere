package org.vadere.util.geometry.mesh.triangulations;

import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriConnectivity;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * @author Benedikt Zoennchen
 *
 * The BasePointLocatetor only uses the mesh itself and does not use any additional data structure
 * to find the face for a given point. It runs a march starting from some (not known) face of the
 * mesh and end up at the face that contains the point. In worst case this is not faster than
 * checking each each face of the mesh but it is more clever and faste in the most cases.
 *
 *
 * @param <P>
 * @param <E>
 * @param <F>
 */
public class BasePointLocator<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> implements IPointLocator<P, E, F> {

	private final ITriConnectivity<P, E, F> triConnectivity;

	public BasePointLocator(final ITriConnectivity<P, E, F> triConnectivity) {
		this.triConnectivity = triConnectivity;
	}

	@Override
	public void splitFaceEvent(final F original, final F[] faces) {}

	@Override
	public void flipEdgeEvent(final F f1, final F f2) {}

	@Override
	public void insertEvent(final P vertex) {}

	@Override
	public void deleteBoundaryFace(final F face) {}

	@Override
	public Collection<F> locatePoint(final IPoint point, boolean insertion) {
		if(insertion) {
			return triConnectivity.getAdjacentFaces(point.getX(), point.getY());
		}
		else {
			Optional<F> optFace = triConnectivity.locate(point.getX(), point.getY());
			return optFace.isPresent() ? Collections.singleton(optFace.get()) : Collections.emptyList();
		}
	}

	@Override
	public Optional<F> locate(final IPoint point) {
		return triConnectivity.locate(point.getX(), point.getY());
	}
}

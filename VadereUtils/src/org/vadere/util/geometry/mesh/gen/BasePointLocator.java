package org.vadere.util.geometry.mesh.gen;

import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriConnectivity;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Optional;

/**
 * @author Benedikt Zoennchen
 *
 * The BasePointLocatetor only uses the mesh itself and does not use any additional data structure
 * to find the face for a given point. It runs a march starting from some (not known) face of the
 * mesh and end up at the face that triangleContains the point. In worst case this is not faster than
 * checking each each face of the mesh but it is more clever and faste in the most cases.
 *
 *
 * @param <P>
 * @param <V>
 * @param <E>
 * @param <F>
 */
public class BasePointLocator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements IPointLocator<P, V, E, F> {

	private ITriConnectivity<P, V, E, F> triConnectivity;

	public BasePointLocator(final ITriConnectivity<P, V, E, F> triConnectivity) {
		this.triConnectivity = triConnectivity;
	}

	@Override
	public void splitTriangleEvent(F original, F f1, F f2, F f3) {}

	@Override
	public void splitEdgeEvent(F original, F f1, F f2) {}

	@Override
	public void flipEdgeEvent(final F f1, final F f2) {}

	@Override
	public void insertEvent(V vertex) {}

	@Override
	public void deleteBoundaryFace(final F face) {}

	@Override
	public F locatePoint(final P point, boolean insertion) {
		//return triConnectivity.getMesh().getFace(triConnectivity.locateNearestNeighbour(point));
		return triConnectivity.locateFace(point.getX(), point.getY()).get();
		/*if(insertion) {
			return triConnectivity.getClosestEdge(point.getX(), point.getY());
		}
		else {
			Optional<F> optFace = triConnectivity.locateFace(point.getX(), point.getY());
			return optFace.isPresent() ? Collections.singleton(optFace.get()) : Collections.emptyList();
		}*/
	}

	@Override
	public Optional<F> locate(final IPoint point) {
		return triConnectivity.locateFace(point.getX(), point.getY());
	}
}

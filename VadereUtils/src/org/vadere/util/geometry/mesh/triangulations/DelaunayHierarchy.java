package org.vadere.util.geometry.mesh.triangulations;

import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriConnectivity;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Collection;
import java.util.Optional;

/**
 * Created by bzoennchen on 21.04.17.
 */
public class DelaunayHierarchy<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> implements IPointLocator<P, E, F>  {

	//private List<ITriangulation<P, E, F>>

	private ITriConnectivity<P, E, F> triConnectivity;

	public DelaunayHierarchy(final ITriConnectivity<P, E, F> triConnectivity) {
		this.triConnectivity = triConnectivity;
	}

	@Override
	public void splitFaceEvent(F original, F[] faces) {}

	@Override
	public void flipEdgeEvent(F f1, F f2) {}

	@Override
	public void insertEvent(P vertex) {

	}

	@Override
	public void deleteBoundaryFace(F face) {

	}

	@Override
	public Collection<F> locatePoint(IPoint point, boolean insertion) {
		return null;
	}

	@Override
	public Optional<F> locate(IPoint point) {
		return null;
	}
}

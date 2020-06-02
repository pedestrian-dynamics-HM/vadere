package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.ITriConnectivity;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Optional;

/**
 * @author Benedikt Zoennchen
 *
 * The {@link BasePointLocator} only uses the mesh itself and does not use any additional data structure
 * to find the face for a given point. It runs a march starting from some arbitrary face of the
 * mesh and end up at the face that contains the point, if there is one. In worst case this is not faster than
 * checking each face of the mesh but it is more clever and faste in most cases.
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class BasePointLocator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IPointLocator<V, E, F> {

	private ITriConnectivity<V, E, F> triConnectivity;

	public BasePointLocator(final ITriConnectivity<V, E, F> triConnectivity) {
		this.triConnectivity = triConnectivity;
	}

	@Override
	public void postSplitTriangleEvent(F original, F f1, F f2, F f3, V v) {}

	@Override
	public void postSplitHalfEdgeEvent(E originalEdge, F original, F f1, F f2, V v) {}

	@Override
	public void postFlipEdgeEvent(final F f1, final F f2) {}

	@Override
	public void postInsertEvent(V vertex) {}

	@Override
	public F locatePoint(@NotNull final IPoint point) {
		//return triConnectivity.getMesh().getFace(triConnectivity.locateNearestNeighbour(point));
		return triConnectivity.locate(point.getX(), point.getY()).get();
		/*if(insertion) {
			return triConnectivity.getClosestEdge(point.getX(), point.getY());
		}
		else {
			Optional<F> optFace = triConnectivity.locate(point.getX(), point.getY());
			return optFace.isPresent() ? Collections.singleton(optFace.get()) : Collections.emptyList();
		}*/
	}

    @Override
    public Optional<F> locate(double x, double y) {
        return locate(triConnectivity.getMesh().createPoint(x, y));
    }

	@Override
	public Type getType() {
		return Type.BASE;
	}

	@Override
	public Optional<F> locate(@NotNull final IPoint point) {
		return triConnectivity.locate(point.getX(), point.getY());
	}
}

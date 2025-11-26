package org.vadere.meshing.mesh.gen.pointLocator;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.ITriangleMeshPointLocator;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Optional;

/**
 * @author Benedikt Zoennchen
 *
 * The {@link SimpleTriangleMeshPointLocator} only uses the mesh itself and does not use any additional data structure
 * to find the face for a given point. It runs a march starting from some arbitrary face of the
 * mesh and end up at the face that contains the point, if there is one. In worst case this is not faster than
 * checking each face of the mesh but it is more clever and faste in most cases.
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class SimpleTriangleMeshPointLocator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITriangleMeshPointLocator<V, E, F> {

	private final ITriangleMesh<V, E, F> triangleMesh;

	public SimpleTriangleMeshPointLocator(final ITriangleMesh<V, E, F> triangleMesh) {
		this.triangleMesh = triangleMesh;
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
		return triangleMesh.readConnectivity().locate(point.getX(), point.getY()).get();
	}

    @Override
    public Optional<F> locate(double x, double y) {
        return triangleMesh.readConnectivity().locate(x, y);
    }

	@Override
	public Type getType() {
		return Type.BASE;
	}

	@Override
	public ITriangleMeshPointLocator<V, E, F> withCache() {
		return new CachedPointLocator<>(this, triangleMesh);
	}

	@Override
	public ITriangleMeshPointLocator<V, E, F> getUncachedLocator() {
		return this;
	}

	@Override
	public boolean isCached() {
		return false;
	}

	@Override
	public ITriangleMeshPointLocator<V, E, F> copyFor(ITriangleMesh<V, E, F> mesh) {
		return new SimpleTriangleMeshPointLocator<>(mesh);
	}

	@Override
	public Optional<F> locate(@NotNull final IPoint point) {
		return triangleMesh.readConnectivity().locate(point.getX(), point.getY());
	}
}

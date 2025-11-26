package org.vadere.meshing.mesh.gen.pointLocator;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.ITriangleMeshPointLocator;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.Optional;
import java.util.Random;

public class JumpAndWalkPointLocator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITriangleMeshPointLocator<V, E, F> {

	private final ITriangleMesh<V, E, F> triangleMesh;
	private Random random;
	private static Logger logger = Logger.getLogger(JumpAndWalkPointLocator.class);

	public JumpAndWalkPointLocator(@NotNull final ITriangleMesh<V, E, F> triangleMesh) {
		this.triangleMesh = triangleMesh;
		this.random = new Random(0);
	}

	private Optional<F> getStartFace(final IPoint endPoint) {
		random = new Random(0);
		int n = triangleMesh.vertices().count();

		if(n < 20) {
			return Optional.empty();
		}
		else {
			V result = null;
			double max = Math.pow(n, 1.0/3.0);
			//double max = n;

			for(int i = 0; i < max; i++) {

				V vertex = triangleMesh.vertices().getRandom(random);

				if(!triangleMesh.faces().isBoundary(triangleMesh.faces().getOf(vertex)) &&
						(result == null || endPoint.distanceSq(vertex) < endPoint.distanceSq(result))) {
					result = vertex;
				}
			}
			return Optional.ofNullable(result == null ? null : triangleMesh.faces().getOf(result));
		}
	}

	@Override
	public F locatePoint(IPoint point) {
		return locate(point).get();
	}

	@Override
	public Optional<F> locate(@NotNull final IPoint point) {
		Optional<F> startFace = getStartFace(point);
		if(startFace.isPresent()) {

			Optional<F> result = triangleMesh.readConnectivity().locateMarch(point.getX(), point.getY(), startFace.get());

			return result;
		}
		else {
			return triangleMesh.readConnectivity().locate(point.getX(), point.getY());
		}
	}

	@Override
	public Optional<F> locate(double x, double y) {
		Optional<F> startFace = getStartFace(new VPoint(x, y));
		if(startFace.isPresent()) {
			return triangleMesh.readConnectivity().locateMarch(x, y, startFace.get());
		}
		else {
			return triangleMesh.readConnectivity().locate(x, y);
		}
	}

	@Override
	public Optional<F> locate(double x, double y, Object caller) {
		Optional<F> startFace = getStartFace(new VPoint(x, y));
		if(startFace.isPresent()) {
			return triangleMesh.readConnectivity().locateMarch(x, y, startFace.get());
		}
		else {
			return triangleMesh.readConnectivity().locate(x, y);
		}
	}

	@Override
	public Type getType() {
		return Type.JUMP_AND_WALK;
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
		return new JumpAndWalkPointLocator<>(mesh);
	}
}

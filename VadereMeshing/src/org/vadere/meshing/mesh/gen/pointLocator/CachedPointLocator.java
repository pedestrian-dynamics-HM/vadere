package org.vadere.meshing.mesh.gen.pointLocator;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.ITriangleMeshPointLocator;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;
import org.vadere.meshing.mesh.inter.mesh.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CachedPointLocator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITriangleMeshPointLocator<V, E, F> {

	private final ITriangleMeshPointLocator<V, E, F> cachedLocator;
	private final ITriangleMesh<V, E, F> mesh;
	private final Map<Object, F> cache;

	public CachedPointLocator(@NotNull final ITriangleMeshPointLocator<V, E, F> pointLocator, @NotNull final ITriangleMesh<V, E, F> mesh) {
		this.cachedLocator = pointLocator;
		this.mesh = mesh;
		this.cache = new ConcurrentHashMap<>();
	}

	@Override
	public F locatePoint(@NotNull final IPoint point) {
		return cachedLocator.locatePoint(point);
	}

	@Override
	public F locatePoint(@NotNull final IPoint point, @NotNull final Object caller) {
		F face;
		if(cache.containsKey(caller) && !mesh.faces().isDestroyed(cache.get(caller))) {
			face = mesh.readConnectivity().locateMarch(point.getX(), point.getY(), cache.get(caller)).orElse(null);
		} else {
			face = cachedLocator.locatePoint(point);
		}

		if(face != null && !mesh.faces().isBoundary(face)) {
			cache.put(caller, face);
		}

		return face;
	}

	@Override
	public Optional<F> locate(@NotNull final IPoint point) {
		return cachedLocator.locate(point);
	}

	@Override
	public Optional<F> locate(@NotNull final IPoint point, final @NotNull Object caller) {
		Optional<F> optFace;
		if(cache.containsKey(caller) && !mesh.faces().isDestroyed(cache.get(caller))) {
			optFace = mesh.readConnectivity().locateMarch(point.getX(), point.getY(), cache.get(caller));
		} else {
			optFace = cachedLocator.locate(point);
		}

		if(optFace.isPresent() && !mesh.faces().isBoundary(optFace.get())) {
			cache.put(caller, optFace.get());
		}

		return optFace;
	}

	@Override
	public Optional<F> locate(double x, double y) {
		return cachedLocator.locate(x, y);
	}

	@Override
	public Optional<F> locate(double x, double y, Object caller) {
		Optional<F> optFace;
		boolean contains = cache.containsKey(caller);
		F starFace = null;

		if(contains) {
			starFace = cache.get(caller);
		}

		if(contains && !mesh.faces().isDestroyed(starFace)) {
			optFace = mesh.readConnectivity().locateMarch(x, y, starFace);
		} else {
			optFace = cachedLocator.locate(x, y, false);
		}

		if(optFace.isPresent() && !(contains && optFace.get().equals(starFace)) &&
				!mesh.faces().isBoundary(optFace.get())) {
			cache.put(caller, optFace.get());
		}

		return optFace;
	}

	@Override
	public Type getType() {
		return cachedLocator.getType();
	}

	@Override
	public ITriangleMeshPointLocator<V, E, F> withCache() {
		return this;
	}

	@Override
	public ITriangleMeshPointLocator<V, E, F> getUncachedLocator() {
		return cachedLocator;
	}

	@Override
	public boolean isCached() {
		return true;
	}

	@Override
	public ITriangleMeshPointLocator<V, E, F> copyFor(ITriangleMesh<V, E, F> mesh) {
		return new CachedPointLocator<>(cachedLocator.copyFor(mesh), mesh);
	}

	@Override
	public void postSplitTriangleEvent(F original, F f1, F f2, F f3, V v) {}

	@Override
	public void postSplitHalfEdgeEvent(E originalEdge, F original, F f1, F f2, V v) {}

	@Override
	public void postFlipEdgeEvent(F f1, F f2) {}

	@Override
	public void postInsertEvent(V vertex) {}
}

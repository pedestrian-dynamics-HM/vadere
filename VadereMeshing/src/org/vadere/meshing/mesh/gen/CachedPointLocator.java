package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.ITriConnectivity;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CachedPointLocator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IPointLocator<V, E, F> {

	private final IPointLocator<V, E, F> pointLocator;
	private final ITriConnectivity<V, E, F> triConnectivity;
	private final Map<Object, F> cache;

	public CachedPointLocator(@NotNull final IPointLocator<V, E, F> pointLocator, @NotNull final ITriConnectivity<V, E, F> triConnectivity) {
		this.pointLocator = pointLocator;
		this.triConnectivity = triConnectivity;
		this.cache = new ConcurrentHashMap<>();
	}

	@Override
	public F locatePoint(@NotNull final IPoint point) {
		return pointLocator.locatePoint(point);
	}

	@Override
	public F locatePoint(@NotNull final IPoint point, @NotNull final Object caller) {
		F face;
		if(cache.containsKey(caller) && !triConnectivity.getMesh().isDestroyed(cache.get(caller))) {
			face = triConnectivity.locateMarch(point.getX(), point.getY(), cache.get(caller)).orElse(null);
		} else {
			face = pointLocator.locatePoint(point);
		}

		if(face != null && !triConnectivity.getMesh().isBoundary(face)) {
			cache.put(caller, face);
		}

		return face;
	}

	@Override
	public Optional<F> locate(@NotNull final IPoint point) {
		return pointLocator.locate(point);
	}

	@Override
	public Optional<F> locate(@NotNull final IPoint point, final @NotNull Object caller) {
		Optional<F> optFace;
		if(cache.containsKey(caller) && !triConnectivity.getMesh().isDestroyed(cache.get(caller))) {
			optFace = triConnectivity.locateMarch(point.getX(), point.getY(), cache.get(caller));
		} else {
			optFace = pointLocator.locate(point);
		}

		if(optFace.isPresent() && !triConnectivity.getMesh().isBoundary(optFace.get())) {
			cache.put(caller, optFace.get());
		}

		return optFace;
	}

	@Override
	public Optional<F> locate(double x, double y) {
		return pointLocator.locate(x, y);
	}

	@Override
	public Optional<F> locate(double x, double y, Object caller) {
		Optional<F> optFace;
		boolean contains = cache.containsKey(caller);
		F starFace = null;

		if(contains) {
			starFace = cache.get(caller);
		}

		if(contains && !triConnectivity.getMesh().isDestroyed(starFace)) {
			optFace = triConnectivity.locateMarch(x, y, starFace);
		} else {
			optFace = pointLocator.locate(x, y, false);
		}

		if(optFace.isPresent() && !(contains && optFace.get().equals(starFace)) &&
				!triConnectivity.getMesh().isBoundary(optFace.get())) {
			cache.put(caller, optFace.get());
		}

		return optFace;
	}

	@Override
	public Type getType() {
		return pointLocator.getType();
	}

	@Override
	public IPointLocator<V, E, F> getUncachedLocator() {
		return pointLocator;
	}

	@Override
	public boolean isCached() {
		return true;
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

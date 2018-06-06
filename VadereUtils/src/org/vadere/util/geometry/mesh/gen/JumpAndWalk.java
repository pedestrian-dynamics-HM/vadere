package org.vadere.util.geometry.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.vadere.util.geometry.mesh.inter.IPointLocator.Type.JUMP_AND_WALK;

/**
 * @author Benedikt Zoennchen
 */
public class JumpAndWalk<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements IPointLocator<P, V, E, F> {

	private final ITriangulation<P, V, E, F> triangulation;
	private final Random random;

	public JumpAndWalk(@NotNull final ITriangulation<P, V, E, F> triangulation) {
		this.triangulation = triangulation;
		this.random = new Random();
	}

	private Optional<F> getStartFace(final IPoint endPoint) {
		Collection<V> vertices = triangulation.getMesh().getVertices();
		int n = vertices.size();

		V result = null;
		int i = 0;
		for(V vertex : vertices) {
			i++;
			if(result == null || endPoint.distanceSq(vertex) < endPoint.distanceSq(result)) {
				result = vertex;
			}

			if(i > Math.sqrt(n)) {
				break;
			}
		}

		if(result == null) {
			return Optional.empty();
		}
		else {
			return Optional.of(triangulation.getMesh().getFace(result));
		}
	}

	@Override
	public F locatePoint(P point, boolean insertion) {
		return locate(point).get();
	}

	@Override
	public Optional<F> locate(P point) {
		Optional<F> startFace = getStartFace(point);
		if(startFace.isPresent()) {
			return triangulation.locateFace(point.getX(), point.getY(), startFace.get());
		}
		else {
			return triangulation.locateFace(point.getX(), point.getY());
		}
	}

	@Override
	public Optional<F> locate(double x, double y) {
		Optional<F> startFace = getStartFace(new VPoint(x, y));
		if(startFace.isPresent()) {
			return triangulation.locateFace(x, y, startFace.get());
		}
		else {
			return triangulation.locateFace(x, y);
		}
	}

	@Override
	public Type getType() {
		return JUMP_AND_WALK;
	}

	@Override
	public void postSplitTriangleEvent(F original, F f1, F f2, F f3) {}

	@Override
	public void postSplitHalfEdgeEvent(F original, F f1, F f2) {}

	@Override
	public void postFlipEdgeEvent(F f1, F f2) {}

	@Override
	public void postInsertEvent(V vertex) {}
}

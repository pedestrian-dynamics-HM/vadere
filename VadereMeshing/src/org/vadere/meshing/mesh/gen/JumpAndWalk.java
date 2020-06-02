package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.Optional;
import java.util.Random;

/**
 * @author Benedikt Zoennchen
 */
public class JumpAndWalk<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IPointLocator<V, E, F> {

	private final IIncrementalTriangulation<V, E, F> triangulation;
	private Random random;
	private static Logger logger = Logger.getLogger(JumpAndWalk.class);

	public JumpAndWalk(@NotNull final IIncrementalTriangulation<V, E, F> triangulation) {
		this.triangulation = triangulation;
		this.random = new Random(0);
	}

	private Optional<F> getStartFace(final IPoint endPoint) {
		random = new Random(0);
		int n = triangulation.getMesh().getNumberOfVertices();

		if(n < 20) {
			return Optional.empty();
		}
		else {
			V result = null;
			double max = Math.pow(n, 1.0/3.0);
			//double max = n;

			for(int i = 0; i < max; i++) {

				V vertex = triangulation.getMesh().getRandomVertex(random);

				if(!triangulation.getMesh().isBoundary(triangulation.getMesh().getFace(vertex)) &&
						(result == null || endPoint.distanceSq(vertex) < endPoint.distanceSq(result))) {
					result = vertex;
				}
			}
			return Optional.ofNullable(result == null ? null : triangulation.getMesh().getFace(result));
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

			Optional<F> result = triangulation.locateMarch(point.getX(), point.getY(), startFace.get());

			/*if(!triangulation.contains(point.getX(), point.getY(), result.get())) {
				result = triangulation.locate(point.getX(), point.getY(), startFace.get());
				List<F> visitedFaces = triangulation.straightGatherWalk2D(point.getX(), point.getY(), startFace.get(), e -> !triangulation.isRightOf(point.getX(), point.getY(), e))
						.stream().map(e -> triangulation.getMesh().getFace(e)).collect(Collectors.toList());
				Function<F, Color> colorFunction = f -> visitedFaces.contains(f) ? Color.GREEN : Color.WHITE;
				logger.debug(TexGraphGenerator.toTikz(triangulation.getMesh(), colorFunction, 1.0f, new VLine(triangulation.getMesh().toTriangle(startFace.get()).midPoint(), new VPoint(point))));
				logger.debug("\n\n");
			}*/

			return result;
		}
		else {
			return triangulation.locate(point.getX(), point.getY());
		}
	}

	@Override
	public Optional<F> locate(double x, double y) {
		Optional<F> startFace = getStartFace(new VPoint(x, y));
		if(startFace.isPresent()) {
			return triangulation.locateMarch(x, y, startFace.get());
		}
		else {
			return triangulation.locate(x, y);
		}
	}

	@Override
	public Optional<F> locate(double x, double y, Object caller) {
		Optional<F> startFace = getStartFace(new VPoint(x, y));
		if(startFace.isPresent()) {
			return triangulation.locateMarch(x, y, startFace.get());
		}
		else {
			return triangulation.locate(x, y);
		}
	}

	@Override
	public Type getType() {
		return Type.JUMP_AND_WALK;
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

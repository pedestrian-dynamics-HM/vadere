package org.vadere.meshing.mesh.gen;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.utils.tex.TexGraphGenerator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Benedikt Zoennchen
 */
public class JumpAndWalk<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements IPointLocator<P, CE, CF, V, E, F> {

	private final IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation;
	private Random random;
	private static Logger logger = LogManager.getLogger(JumpAndWalk.class);

	public JumpAndWalk(@NotNull final IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation) {
		this.triangulation = triangulation;
		this.random = new Random();
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
	public F locatePoint(P point) {
		return locate(point).get();
	}

	@Override
	public Optional<F> locate(P point) {
		Optional<F> startFace = getStartFace(point);
		if(startFace.isPresent()) {

			Optional<F> result = triangulation.locateFace(point.getX(), point.getY(), startFace.get());

			/*if(!triangulation.contains(point.getX(), point.getY(), result.get())) {
				result = triangulation.locateFace(point.getX(), point.getY(), startFace.get());
				List<F> visitedFaces = triangulation.straightGatherWalk2D(point.getX(), point.getY(), startFace.get(), e -> !triangulation.isRightOf(point.getX(), point.getY(), e))
						.stream().map(e -> triangulation.getMesh().getFace(e)).collect(Collectors.toList());
				Function<F, Color> colorFunction = f -> visitedFaces.contains(f) ? Color.GREEN : Color.WHITE;
				logger.debug(TexGraphGenerator.toTikz(triangulation.getMesh(), colorFunction, 1.0f, new VLine(triangulation.getMesh().toTriangle(startFace.get()).midPoint(), new VPoint(point))));
				logger.debug("\n\n");
			}*/

			return result;
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
		return Type.JUMP_AND_WALK;
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

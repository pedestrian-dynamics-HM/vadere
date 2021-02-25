package org.vadere.meshing.mesh.triangulation.edgeLengthFunctions;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.DataPoint;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.math.InterpolationUtil;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Function;

/**
 * The edge-length function used in {@link GenEikMesh},
 * {@link org.vadere.meshing.mesh.triangulation.improver.distmesh.Distmesh} and some
 * {@link ITriangulator} gives for every position
 * in the 2D Euclidean space the desired relative length of an edge of a mesh. Relative in the sense
 * that if the function is a constant equals to c edges will be of approximately the same length and
 * the actual length does not rely on c.
 *
 * @author Benedikt Zoennchen
 */
@FunctionalInterface
public interface IEdgeLengthFunction extends Function<IPoint,Double> {

	static final String propName = "edgeLength";

	static IEdgeLengthFunction createLFS(@NotNull final PSLG pslg) {
		double g = 2.0;
		IPointConstructor<DataPoint<Double>> pointConstructor = (x, y) -> new DataPoint<>(x, y);
		var ruppertsTriangulator = new PRuppertsTriangulator(
				pslg,
				10
		);

		final var triangulation = ruppertsTriangulator.generate();
		var mesh = triangulation.getMesh();

		IEdgeLengthFunction edgeLengthFunction = p -> {
			var face = triangulation.locate(p.getX(), p.getY()).get();
			if(mesh.isBoundary(face)) {
				return Double.POSITIVE_INFINITY;
			}
			else {
				//TODO dulicated code see EdgeLengthFunction
				double x[] = new double[3];
				double y[] = new double[3];
				double z[] = new double[3];

				triangulation.getTriPoints(face, x, y, z, propName);

				double totalArea = GeometryUtils.areaOfPolygon(x, y);

				return InterpolationUtil.barycentricInterpolation(x, y, z, totalArea, p.getX(), p.getY());
			}
		};
		return edgeLengthFunction;
	}

	default <V extends IVertex, E extends IHalfEdge, F extends IFace> void smooth(double g, IIncrementalTriangulation<V, E, F> triangulation) {
		assert g > 0;
		// smooth the function based such that it is g-Lipschitz
		var mesh = triangulation.getMesh();
		PriorityQueue<V> heap = new PriorityQueue<>(
				Comparator.comparingDouble(v1 -> mesh.getDoubleData(v1, propName))
		);
		heap.addAll(mesh.getVertices());

		while (!heap.isEmpty()) {
			var v = heap.poll();
			double hv = mesh.getDoubleData(v, propName);
			for (var u : mesh.getAdjacentVertexIt(v)) {
				double hu = mesh.getDoubleData(u, propName);
				double min = Math.min(hu, hv + g * v.distance(u));

				// update heap
				if (min < hu) {
					heap.remove(u);
					mesh.setDoubleData(u, propName, min);
					heap.add(u);
				}
			}
		}
	}

	/*static IEdgeLengthFunction smooth(double g) {

		PriorityQueue<PVertex<DataPoint<Double>,Object, Object>> heap = new PriorityQueue<>();
		while (heap.isEmpty()) {
			var u = heap.poll();
			var dataPoint = mesh.getPoint(u);

			for(var v : mesh.getAdjacentVertexIt(u)) {
				double hv = Math.min(mesh.getPoint(v).getData(), dataPoint.getData() + g * dataPoint.distance(v.getX(), v.getY()));
				if(hv < mesh.getPoint(v).getData()) {
					heap.remove(v);
					mesh.getPoint(v).setData(hv);
					heap.add(v);
				}
			}
		}
	}*/
}

package org.vadere.meshing.mesh.triangulation;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PMeshSuppliert;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.DataPoint;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRuppertsTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.Geometry;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.InterpolationUtil;

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

	static IEdgeLengthFunction createLFS(@NotNull final PSLG pslg) {
		double g = 2.0;
		IPointConstructor<DataPoint<Double>> pointConstructor = (x, y) -> new DataPoint<>(x, y);
		var ruppertsTriangulator = new PRuppertsTriangulator<>(
				pslg,
				pointConstructor,
				10
		);

		final var triangulation = ruppertsTriangulator.generate();
		var mesh = triangulation.getMesh();

		IEdgeLengthFunction edgeLengthFunction = p -> {
			var face = triangulation.locateFace(p.getX(), p.getY()).get();
			if(mesh.isBoundary(face)) {
				return Double.POSITIVE_INFINITY;
			}
			else {
				var edge = mesh.getEdge(face);
				DataPoint<Double> p1 = mesh.getPoint(edge);
				DataPoint<Double> p2 = mesh.getPoint(mesh.getNext(edge));
				DataPoint<Double> p3 = mesh.getPoint(mesh.getPrev(edge));
				return InterpolationUtil.barycentricInterpolation(p1, p2, p3, dataPoint -> dataPoint.getData(), p.getX(), p.getY());
			}
		};
		return edgeLengthFunction;
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

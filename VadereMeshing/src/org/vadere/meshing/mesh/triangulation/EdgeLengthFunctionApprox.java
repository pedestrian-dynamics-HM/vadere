package org.vadere.meshing.mesh.triangulation;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.DataPoint;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.math.InterpolationUtil;

import java.util.Comparator;
import java.util.PriorityQueue;

public class EdgeLengthFunctionApprox implements IEdgeLengthFunction {

	private IIncrementalTriangulation<DataPoint<Double>, Object, Object, PVertex<DataPoint<Double>, Object, Object>, PHalfEdge<DataPoint<Double>, Object, Object>, PFace<DataPoint<Double>, Object, Object>> triangulation;

	public EdgeLengthFunctionApprox(@NotNull final PSLG pslg) {

		IPointConstructor<DataPoint<Double>> pointConstructor = (x, y) -> new DataPoint<>(x, y);
		var ruppertsTriangulator = new PRuppertsTriangulator<>(pslg, pointConstructor, 10);
		triangulation = ruppertsTriangulator.generate();

		//TODO: maybe transform into an immutable triangulation / mesh!
		triangulation.setCanIllegalPredicate(e -> true);

		// compute and set the local feature size
		var vertices = triangulation.getMesh().getVertices();

		for(var v : vertices) {
			double minEdgeLen = Double.MAX_VALUE;
			for(var u : triangulation.getMesh().getAdjacentVertexIt(v)) {
				double len = v.distance(u);
				if(len < minEdgeLen) {
					minEdgeLen = len;
				}
			}

			triangulation.getMesh().getPoint(v).setData(minEdgeLen);
		}
	}

	public void smooth(double g) {
		assert g > 0;
		// smooth the function based such that it is g-Lipschitz
		var mesh = triangulation.getMesh();
		PriorityQueue<PVertex<DataPoint<Double>, Object, Object>> heap = new PriorityQueue<>(
				Comparator.comparingDouble(v1 -> mesh.getPoint(v1).getData())
		);
		heap.addAll(mesh.getVertices());

		while (!heap.isEmpty()) {
			var v = heap.poll();
			double hv = mesh.getPoint(v).getData();
			for (var u : mesh.getAdjacentVertexIt(v)) {
				double hu = mesh.getPoint(u).getData();
				double min = Math.min(hu, hv + g * v.distance(u));

				// update heap
				if (min < hu) {
					heap.remove(u);
					mesh.getPoint(u).setData(min);
					heap.add(u);
				}
			}
		}
	}

	@Override
	public Double apply(@NotNull final IPoint p) {
		var face = triangulation.locateFace(p.getX(), p.getY()).get();
		var mesh = triangulation.getMesh();

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
	}

	public void printPython() {
		System.out.println(triangulation.getMesh().toPythonTriangulation(dataPoint -> dataPoint.getData()));
		/*var points = triangulation.getMesh().getPoints();
		System.out.print("[");
		for(var dataPoint : points) {
			System.out.print("["+dataPoint.getX()+","+dataPoint.getY()+"],");
		}
		System.out.println("]\n\n");

		System.out.print("[");
		for(var dataPoint : points) {
			System.out.print(dataPoint.getData()+",");
		}
		System.out.println("]");*/
	}
}

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
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.math.InterpolationUtil;

import java.util.Comparator;
import java.util.PriorityQueue;

public class EdgeLengthFunctionApprox implements IEdgeLengthFunction {

	private IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation;

	private static final String propName = "edgeLength";

	public EdgeLengthFunctionApprox(@NotNull final PSLG pslg) {

		//IPointConstructor<DataPoint<Double>> pointConstructor = (x, y) -> new DataPoint<>(x, y);
		var ruppertsTriangulator = new PRuppertsTriangulator(pslg, 10);
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

			triangulation.getMesh().setData(v, propName, minEdgeLen);
		}
	}

	public void smooth(double g) {
		assert g > 0;
		// smooth the function based such that it is g-Lipschitz
		var mesh = triangulation.getMesh();
		PriorityQueue<PVertex> heap = new PriorityQueue<>(
				Comparator.comparingDouble(v1 -> mesh.getData(v1, propName, Double.class).get())
		);
		heap.addAll(mesh.getVertices());

		while (!heap.isEmpty()) {
			var v = heap.poll();
			double hv = mesh.getData(v, propName, Double.class).get();
			for (var u : mesh.getAdjacentVertexIt(v)) {
				double hu = mesh.getData(u, propName, Double.class).get();
				double min = Math.min(hu, hv + g * v.distance(u));

				// update heap
				if (min < hu) {
					heap.remove(u);
					mesh.setData(u, propName, min);
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
			double x[] = new double[3];
			double y[] = new double[3];
			double z[] = new double[3];

			triangulation.getTriPoints(face, x, y, z, propName);

			double totalArea = GeometryUtils.areaOfPolygon(x, y);

			return InterpolationUtil.barycentricInterpolation(x, y, z, totalArea, p.getX(), p.getY());
		}
	}

	public void printPython() {
		System.out.println(triangulation.getMesh().toPythonTriangulation(v -> triangulation.getMesh().getData(v, propName, Double.class).get()));
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

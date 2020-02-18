package org.vadere.meshing.mesh.triangulation;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.IDistanceFunctionCached;
import org.vadere.util.math.InterpolationUtil;
import java.util.function.Function;

public class DistanceFunctionApproxBF implements IDistanceFunctionCached {
	private IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation;

	private static final String propName = "distObs";

	public DistanceFunctionApproxBF(
			@NotNull final PSLG pslg,
			@NotNull final Function<IPoint, Double> circumRadiusFunc,
			@NotNull final IDistanceFunction exactDistanceFunc) {
		//IPointConstructor<DataPoint<Double>> pointConstructor = (x, y) -> new DataPoint<>(x, y);
		/**
		 * Add a bound around so the edge function is also defined outside.
		 */
		VRectangle bound = GeometryUtils.boundRelativeSquared(pslg.getSegmentBound().getPoints(), 0.3);
		PSLG boundedPSLG = pslg.conclose(bound);

		var ruppertsTriangulator = new PRuppertsTriangulator(boundedPSLG, circumRadiusFunc, 10, false, false);
		triangulation = ruppertsTriangulator.generate();

		//TODO: maybe transform into an immutable triangulation / mesh!
		triangulation.setCanIllegalPredicate(e -> true);

		// compute and set the local feature size
		var vertices = triangulation.getMesh().getVertices();

		for(var v : vertices) {
			double distance = exactDistanceFunc.apply(v);
			triangulation.getMesh().setDoubleData(v, propName, distance);
		}
	}

	public DistanceFunctionApproxBF(@NotNull final PSLG pslg, @NotNull final IDistanceFunction exactDistanceFunc) {
		this(pslg, p -> Double.POSITIVE_INFINITY, exactDistanceFunc);
		//IPointConstructor<DataPoint<Double>> pointConstructor = (x, y) -> new DataPoint<>(x, y);
	}

	@Override
	public Double apply(@NotNull final IPoint p) {
		var face = triangulation.locateFace(new VPoint(p.getX(), p.getY())).get();
		return apply(p, face);
	}

	public void printPython() {
		System.out.println(triangulation.getMesh().toPythonTriangulation(v -> triangulation.getMesh().getDoubleData(v, propName)));
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

	@Override
	public double apply(@NotNull IPoint p, Object caller) {
		var face = triangulation.locateFace(new VPoint(p.getX(), p.getY()), caller).get();
		return apply(p, face);
	}

	private double apply(@NotNull final IPoint p, @NotNull final PFace face) {
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
}

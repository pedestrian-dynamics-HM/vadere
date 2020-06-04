package org.vadere.meshing.mesh.triangulation;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRuppertsTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.IDistanceFunctionCached;
import org.vadere.util.math.InterpolationUtil;
import java.util.function.Function;

public class DistanceFunctionApproxBF<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IDistanceFunctionCached {
	private IIncrementalTriangulation<V, E, F> triangulation;

	private static final String propName = "distObs";

	public DistanceFunctionApproxBF(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final IDistanceFunction exactDistanceFunc) {

		this.triangulation = new IncrementalTriangulation<>(mesh);
		this.triangulation.enableCache();
		//TODO: maybe transform into an immutable triangulation / mesh!
		triangulation.setCanIllegalPredicate(e -> true);

		// compute and set the local feature size
		var vertices = triangulation.getMesh().getVertices();

		for(var v : vertices) {
			double distance = exactDistanceFunc.apply(v);
			triangulation.getMesh().setDoubleData(v, propName, distance);
		}
	}

	public DistanceFunctionApproxBF(
			@NotNull final PSLG pslg,
			@NotNull final Function<IPoint, Double> circumRadiusFunc,
			@NotNull final IDistanceFunction exactDistanceFunc,
			@NotNull final IMeshSupplier<V, E, F> meshSupplier) {
		//IPointConstructor<DataPoint<Double>> pointConstructor = (x, y) -> new DataPoint<>(x, y);
		/**
		 * Add a bound around so the edge function is also defined outside.
		 */
		VRectangle bound = GeometryUtils.boundRelativeSquared(pslg.getSegmentBound().getPoints(), 0.3);
		PSLG boundedPSLG = pslg.conclose(bound);

		var ruppertsTriangulator = new GenRuppertsTriangulator<V, E, F>(meshSupplier, boundedPSLG,10, circumRadiusFunc, false, false);
		triangulation = ruppertsTriangulator.generate();
		triangulation.enableCache();

		//TODO: maybe transform into an immutable triangulation / mesh!
		triangulation.setCanIllegalPredicate(e -> true);

		// compute and set the local feature size
		var vertices = triangulation.getMesh().getVertices();

		for(var v : vertices) {
			double distance = exactDistanceFunc.apply(v);
			triangulation.getMesh().setDoubleData(v, propName, distance);
		}
	}

	public DistanceFunctionApproxBF(@NotNull final PSLG pslg, @NotNull final IDistanceFunction exactDistanceFunc, @NotNull final IMeshSupplier<V, E, F> meshSupplier) {
		this(pslg, p -> Double.POSITIVE_INFINITY, exactDistanceFunc, meshSupplier);
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
	public double apply(@NotNull final IPoint p, Object caller) {
		var face = triangulation.locateFace(new VPoint(p.getX(), p.getY()), caller).get();
		return apply(p, face);
	}

	private double apply(@NotNull final IPoint p, @NotNull final F face) {
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

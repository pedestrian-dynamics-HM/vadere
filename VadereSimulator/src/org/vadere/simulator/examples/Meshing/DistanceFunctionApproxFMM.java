package org.vadere.simulator.examples.Meshing;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverFMM;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.IDistanceFunctionCached;
import org.vadere.util.math.InterpolationUtil;

import java.util.function.Function;
import java.util.stream.Collectors;

public class DistanceFunctionApproxFMM implements IDistanceFunctionCached {
	private IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation;

	private static final String propName = "distObs";
	private MeshEikonalSolverFMM eikSolver;
	private final PSLG pslg;
	public DistanceFunctionApproxFMM(
			@NotNull final PSLG pslg,
			@NotNull final Function<IPoint, Double> circumRadiusFunc) {
		//IPointConstructor<DataPoint<Double>> pointConstructor = (x, y) -> new DataPoint<>(x, y);
		/**
		 * Add a bound around so the edge function is also defined outside.
		 */
		this.pslg = pslg;
		VRectangle bound = GeometryUtils.boundRelativeSquared(pslg.getSegmentBound().getPoints(), 0.3);
		PSLG boundedPSLG = pslg.conclose(bound);

		var ruppertsTriangulator = new PRuppertsTriangulator(boundedPSLG, circumRadiusFunc, 10, false, false);
		triangulation = ruppertsTriangulator.generate();

		//TODO: maybe transform into an immutable triangulation / mesh!
		triangulation.setCanIllegalPredicate(e -> true);

		// compute and set the local feature size
		var vertices = ruppertsTriangulator.getSegments().stream().map(e -> triangulation.getMesh().getVertex(e)).collect(Collectors.toList());

		/*for(var v : vertices) {
			double distance = exactDistanceFunc.apply(v);
			triangulation.getMesh().setDoubleData(v, propName, distance);
		}*/

		eikSolver = new MeshEikonalSolverFMM(p -> 1, triangulation, vertices);
		eikSolver.solve();

		// the distance is negative inside holes and outside the domain
		// TODO this is very slow and should be implemented much more efficiently
		for(var v : triangulation.getMesh().getVertices()) {
			VPoint p = triangulation.getMesh().toPoint(v);
			if(pslg.getSegmentBound().contains(v) && pslg.getHoles().stream().noneMatch(poly -> poly.contains(p))) {
				eikSolver.setPotential(v, -eikSolver.getPotential(v));
			}
		}
	}

	/**
	 * public EikonalSolverFMMTriangulation(@NotNull final ITimeCostFunction timeCostFunction,
	 *  @NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	 * 	@NotNull final Collection<V> targetVertices
	 * @param pslg
	 */
	public DistanceFunctionApproxFMM(@NotNull final PSLG pslg) {
		this(pslg, p -> Double.POSITIVE_INFINITY);
		//IPointConstructor<DataPoint<Double>> pointConstructor = (x, y) -> new DataPoint<>(x, y);
	}

	@Override
	public Double apply(@NotNull final IPoint p) {
		var face = triangulation.locateFace(new VPoint(p.getX(), p.getY())).get();
		return apply(p, face);
	}

	public void printPython() {
		System.out.println(triangulation.getMesh().toPythonTriangulation(v -> eikSolver.getPotential(v)));
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
			return Double.NEGATIVE_INFINITY;
		}
		else {
			double x[] = new double[3];
			double y[] = new double[3];
			double z[] = new double[3];

			triangulation.getTriPoints(face, x, y, z, MeshEikonalSolverFMM.namePotential);

			double totalArea = GeometryUtils.areaOfPolygon(x, y);

			return InterpolationUtil.barycentricInterpolation(x, y, z, totalArea, p.getX(), p.getY());
		}
	}
}

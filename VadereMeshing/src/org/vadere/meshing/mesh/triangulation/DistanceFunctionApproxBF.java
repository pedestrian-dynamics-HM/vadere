package org.vadere.meshing.mesh.triangulation;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertexContainerDouble;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRuppertsTriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.IDistanceFunctionCached;
import org.vadere.util.math.InterpolationUtil;
import java.util.function.Function;
import java.util.function.Supplier;

public class DistanceFunctionApproxBF<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IDistanceFunctionCached {
	private IIncrementalTriangulation<V, E, F> triangulation;

	private static final String propName = "distObs";
	private IVertexContainerDouble<V, E, F> distances;

	public DistanceFunctionApproxBF(
			@NotNull final ITriangleMeshBuilder<V, E, F> meshWithDataStorage,
			@NotNull final IDistanceFunction exactDistanceFunc) {

		this.triangulation = IncrementalTriangulation.fromMeshBuilder(meshWithDataStorage);
		this.triangulation.enablePointLocatorCache();
		this.triangulation.setCanIllegalPredicate(e -> true);
		this.distances = triangulation.getMeshDataStorage().getDoubleVertexContainer(propName, getMesh());
		// compute and set the local feature size
		var vertices = getMesh().vertices().getAll();

		for(var v : vertices) {
			double distance = exactDistanceFunc.apply(v);
			this.distances.setValue(v, distance);
		}
	}

	public DistanceFunctionApproxBF(
			@NotNull final PSLG pslg,
			@NotNull final Function<IPoint, Double> circumRadiusFunc,
			@NotNull final IDistanceFunction exactDistanceFunc,
			@NotNull final Supplier<ITriangleMeshBuilder<V, E, F>> meshSupplier) {
		//IPointConstructor<DataPoint<Double>> pointConstructor = (x, y) -> new DataPoint<>(x, y);
		/**
		 * Add a bound around so the edge function is also defined outside.
		 */
		VRectangle bound = GeometryUtils.boundRelativeSquared(pslg.getSegmentBound().getPoints(), 0.3);
		PSLG boundedPSLG = pslg.conclose(bound);

		var ruppertsTriangulator = new GenRuppertsTriangulator<V, E, F>(meshSupplier, boundedPSLG,10, circumRadiusFunc, false, false);
		this.triangulation = ruppertsTriangulator.generate();
		this.triangulation.enablePointLocatorCache();
		this.distances = triangulation.getMeshDataStorage().getDoubleVertexContainer(propName, getMesh());

		//TODO: maybe transform into an immutable triangulation / mesh!
		this.triangulation.setCanIllegalPredicate(e -> true);

		// compute and set the local feature size
		var vertices = triangulation.getVertices();

		for(var v : vertices) {
			double distance = exactDistanceFunc.apply(v);
			this.triangulation.getMeshDataStorage().setDoubleData(v, propName, distance);
		}
	}

	public DistanceFunctionApproxBF(@NotNull final PSLG pslg, @NotNull final IDistanceFunction exactDistanceFunc, @NotNull final Supplier<ITriangleMeshBuilder<V, E, F>> meshSupplier) {
		this(pslg, p -> Double.POSITIVE_INFINITY, exactDistanceFunc, meshSupplier);
		//IPointConstructor<DataPoint<Double>> pointConstructor = (x, y) -> new DataPoint<>(x, y);
	}

	@Override
	public Double apply(@NotNull final IPoint p) {
		var face = triangulation.locateFace(new VPoint(p.getX(), p.getY())).get();
		return apply(p, face);
	}

	public void printPython() {
		System.out.println(MeshPythonUtils.toPythonTriangulation(triangulation.getMeshBuilder().getMeshWithDataStorage(), propName));
	}

	@Override
	public double apply(@NotNull final IPoint p, Object caller) {
		var face = triangulation.locateFace(new VPoint(p.getX(), p.getY()), caller).get();
		return apply(p, face);
	}

	private IMesh<V, E, F> getMesh() {
		return triangulation.getMesh();
	}

	private double apply(@NotNull final IPoint p, @NotNull final F face) {
		var mesh = triangulation.getMeshBuilder();

		if(mesh.getMesh().faces().isBoundary(face)) {
			return Double.POSITIVE_INFINITY;
		}
		else {
			E edge = getMesh().edges().getAnyOf(face);
			V v = getMesh().vertices().getEndOf(edge);
			double x1 = getMesh().vertices().getX(v);
			double y1 = getMesh().vertices().getY(v);
			double val1 = distances.getValue(v);

			v = getMesh().vertices().getEndOf(getMesh().edges().getNext(edge));
			double x2 = getMesh().vertices().getX(v);
			double y2 = getMesh().vertices().getY(v);
			double val2 = distances.getValue(v);

			v = getMesh().vertices().getEndOf(getMesh().edges().getPrev(edge));
			double x3 = getMesh().vertices().getX(v);
			double y3 = getMesh().vertices().getY(v);
			double val3 = distances.getValue(v);

			double totalArea = GeometryUtils.areaOfTriangle(x1, y1, x2, y2, x3, y3);
			return InterpolationUtil.barycentricInterpolation(x1, y1, val1, x2, y2, val2, x3, y3, val3, totalArea, p.getX(), p.getY());
		}
	}
}

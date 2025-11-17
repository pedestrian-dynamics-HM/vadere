package org.vadere.meshing.mesh.triangulation.edgeLengthFunctions;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.PVertex;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.MeshPythonUtils;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.InterpolationUtil;

import java.util.function.Function;

public class EdgeLengthFunctionApprox implements IEdgeLengthFunction {

	private IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation;

	public EdgeLengthFunctionApprox(
			@NotNull final PSLG pslg,
			@NotNull final Function<IPoint, Double> circumRadiusFunc) {
		this(pslg, circumRadiusFunc, p -> Double.POSITIVE_INFINITY);
	}

	public EdgeLengthFunctionApprox(
			@NotNull final PSLG pslg,
			@NotNull final Function<IPoint, Double> circumRadiusFunc,
			final IEdgeLengthFunction edgeLengthFunction) {

		//IPointConstructor<DataPoint<Double>> pointConstructor = (x, y) -> new DataPoint<>(x, y);
		/**
		 * Add a bound around so the edge function is also defined outside.
		 */
		VRectangle bound = GeometryUtils.boundRelativeSquared(pslg.getSegmentBound().getPoints(), 0.3);
		PSLG boundedPSLG = pslg.conclose(bound);

		var ruppertsTriangulator = new PRuppertsTriangulator(boundedPSLG, pslg, circumRadiusFunc, 10, false, false);
		triangulation = ruppertsTriangulator.generate();
		triangulation.enableCache();

		//TODO: maybe transform into an immutable triangulation / mesh!
		triangulation.setCanIllegalPredicate(e -> true);

		// compute and set the local feature size
		var vertices = triangulation.getMesh().getVertices();
		var meshWithDataStorage = triangulation.getMeshWithDataStorage();
		var dataStorage = meshWithDataStorage.getDataStorage();
		for(var v : vertices) {
			double minEdgeLen = Double.MAX_VALUE;
			for(var e : triangulation.getMesh().getEdges(v)) {
				if(!dataStorage.getBooleanData(meshWithDataStorage.getMesh().getFace(e), "boundary")
						|| !dataStorage.getBooleanData(meshWithDataStorage.getMesh().getTwinFace(e), "boundary")) {
					var u = triangulation.getMeshWithDataStorage().getMesh().getTwinVertex(e);
					double len = v.distance(u) * (1.0 / (Math.sqrt(2) * 1.2/*4.0*/));
					if(len < minEdgeLen) {
						minEdgeLen = len;
					}
				}
			}

			triangulation.getMeshWithDataStorage().getDataStorage().setDoubleData(v, propName, Math.min(edgeLengthFunction.apply(v), minEdgeLen));
		}
	}

	public EdgeLengthFunctionApprox(@NotNull final PSLG pslg) {
		this(pslg, p -> Double.POSITIVE_INFINITY, p -> Double.POSITIVE_INFINITY);
		//IPointConstructor<DataPoint<Double>> pointConstructor = (x, y) -> new DataPoint<>(x, y);
	}

	public IMesh<PVertex, PHalfEdge, PFace> getMesh() {
		return triangulation.getMesh();
	}

	public void smooth(double g) {
		assert g > 0;
		smooth(g, triangulation);
	}

	@Override
	public Double apply(@NotNull final IPoint p) {
		var face = triangulation.locateFace(new VPoint(p.getX(), p.getY())).get();
		var mesh = triangulation.getMeshWithDataStorage();

		if(mesh.getMesh().isBoundary(face)) {
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
		IMeshWithDataStorage<PVertex, PHalfEdge, PFace> meshWithDataStorage = triangulation.getMeshWithDataStorage();
		String str = MeshPythonUtils.toPythonTriangulation(meshWithDataStorage, propName);
		System.out.println(str);
	}
}

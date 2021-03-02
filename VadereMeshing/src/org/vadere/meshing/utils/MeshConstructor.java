package org.vadere.meshing.utils;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.MeshRenderer;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.triangulation.DistanceFunctionApproxBF;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.EdgeLengthFunctionApprox;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.meshing.utils.color.Colors;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.util.Collection;
import java.util.function.Function;

public class MeshConstructor {
	private static Logger logger = Logger.getLogger(MeshConstructor.class);

	public IMesh<PVertex, PHalfEdge, PFace> pslgToCoarsePMesh(@NotNull final PSLG pslg, final Function<IPoint, Double> circumRadiusFunc, final boolean viszalize) {
		VRectangle bound = GeometryUtils.boundRelativeSquared(pslg.getSegmentBound().getPoints(), 0.3);
		PSLG boundedPSLG = pslg.conclose(bound);

		var ruppertsTriangulator = new PRuppertsTriangulator(boundedPSLG, pslg, circumRadiusFunc, 10, false, false);

		IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation;

		if(viszalize) {
			var meshRenderer = new MeshRenderer<>(ruppertsTriangulator.getMesh(), f -> false, f -> Colors.YELLOW, e -> Color.BLACK, v -> Color.BLACK);
			var meshPanel = new PMeshPanel(meshRenderer, 600, 600);
			meshPanel.display("Ruppert's algorithm");
			while(!ruppertsTriangulator.isFinished()) {
				synchronized (ruppertsTriangulator.getMesh()) {
					ruppertsTriangulator.step();
				}
				meshPanel.repaint();
			}
			triangulation = ruppertsTriangulator.getTriangulation();
		} else {
			triangulation = ruppertsTriangulator.generate();
		}

		return triangulation.getMesh();
	}

	public IMesh<PVertex, PHalfEdge, PFace> pslgToAdaptivePMesh(
			@NotNull final PSLG pslg, final double hmin, final double hmax, final boolean viszalize) {

		double smoothness = 0.4;
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();
		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);
		logger.info("construct distance function");
		IDistanceFunction distanceFunctionApproximation = new DistanceFunctionApproxBF(pslg, distanceFunction, () -> new PMesh());

		IEdgeLengthFunction edgeLengthFunction = p -> hmin + smoothness * Math.abs((distanceFunctionApproximation).apply(p));
		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg, edgeLengthFunction, p -> hmax);
		edgeLengthFunctionApprox.smooth(smoothness);
		logger.info("construct element size function");

		Collection<VPolygon> polygons = pslg.getAllPolygons();
		//polygons.add(targetShape);

		// (3) use EikMesh to improve the mesh
		double h0 = hmin;
		var meshImprover = new PEikMesh(
				distanceFunctionApproximation,
				edgeLengthFunctionApprox,
				h0,
				pslg.getBoundingBox(),
				polygons
		);

		if(viszalize) {
			Function<PVertex, Color> vertexColorFunction = v -> {
				if(meshImprover.isSlidePoint(v)){
					return Colors.BLUE;
				} else if(meshImprover.isFixPoint(v)) {
					return Colors.RED;
				} else {
					return Color.BLACK;
				}
			};

			var meshRenderer = new MeshRenderer<>(meshImprover.getMesh(), f -> false, f -> Colors.YELLOW, e -> Color.BLACK, vertexColorFunction);
			var meshPanel = new PMeshPanel(meshRenderer, 500, 500);
			meshPanel.display("EikMesh h0 = " + h0);

			while (!meshImprover.isFinished()) {
				synchronized (meshImprover.getMesh()) {
					meshImprover.improve();
					logger.info("quality = " + meshImprover.getQuality());
				}
				meshPanel.repaint();
			}
			logger.info("generation completed.");

		} else {
			meshImprover.generate();
		}
		return meshImprover.getMesh();
	}

	public IMesh<PVertex, PHalfEdge, PFace> pslgToUniformPMesh(@NotNull final PSLG pslg, final double h0, final boolean viszalize) {
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();
		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);
		logger.info("construct distance function");
		IDistanceFunction distanceFunctionApproximation = new DistanceFunctionApproxBF(pslg, distanceFunction, () -> new PMesh());


		Collection<VPolygon> polygons = pslg.getAllPolygons();
		//polygons.add(targetShape);

		// (3) use EikMesh to improve the mesh
		var meshImprover = new PEikMesh(
				distanceFunctionApproximation,
				p -> h0,
				h0,
				pslg.getBoundingBox(),
				polygons
		);

		if(viszalize) {
			Function<PVertex, Color> vertexColorFunction = v -> {
				if(meshImprover.isSlidePoint(v)){
					return Colors.BLUE;
				} else if(meshImprover.isFixPoint(v)) {
					return Colors.RED;
				} else {
					return Color.BLACK;
				}
			};

			var meshRenderer = new MeshRenderer<>(meshImprover.getMesh(), f -> false, f -> Colors.YELLOW, e -> Color.BLACK, vertexColorFunction);
			var meshPanel = new PMeshPanel(meshRenderer, 500, 500);
			meshPanel.display("EikMesh uniform h0 = " + h0);

			while (!meshImprover.isFinished()) {
				synchronized (meshImprover.getMesh()) {
					meshImprover.improve();
				}
				meshPanel.repaint();
			}
		} else {
			meshImprover.generate();
		}
		return meshImprover.getMesh();
	}

	public IMesh<PVertex, PHalfEdge, PFace> pslgToUniformOptimalPMesh(@NotNull final PSLG pslg, final double h0, final boolean viszalize) {
		double smoothness = 0.2;
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();
		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);
		logger.info("construct distance function");
		IDistanceFunction distanceFunctionApproximation = new DistanceFunctionApproxBF(pslg, distanceFunction, () -> new PMesh());
		((DistanceFunctionApproxBF) distanceFunctionApproximation).printPython();

		//IEdgeLengthFunction circumRadiusFunc = p -> h0 / 2.0;
		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg, p -> Double.POSITIVE_INFINITY, p -> h0);
		edgeLengthFunctionApprox.smooth(smoothness);
		logger.info("construct element size function");
		edgeLengthFunctionApprox.printPython();

		Collection<VPolygon> polygons = pslg.getAllPolygons();
		//polygons.add(targetShape);

		// (3) use EikMesh to improve the mesh
		var meshImprover = new PEikMesh(
				distanceFunctionApproximation,
				edgeLengthFunctionApprox,
				h0,
				pslg.getBoundingBox(),
				polygons
		);

		if(viszalize) {
			Function<PVertex, Color> vertexColorFunction = v -> {
				if(meshImprover.isSlidePoint(v)){
					return Colors.BLUE;
				} else if(meshImprover.isFixPoint(v)) {
					return Colors.RED;
				} else {
					return Color.BLACK;
				}
			};

			var meshRenderer = new MeshRenderer<>(meshImprover.getMesh(), f -> false, f -> Colors.YELLOW, e -> Color.BLACK, vertexColorFunction);
			var meshPanel = new PMeshPanel(meshRenderer, 500, 500);
			meshPanel.display("EikMesh h0 = " + h0);

			while (!meshImprover.isFinished()) {
				synchronized (meshImprover.getMesh()) {
					meshImprover.improve();
					logger.info("quality = " + meshImprover.getQuality());
				}
				meshPanel.repaint();
			}
			logger.info("generation completed.");

		} else {
			meshImprover.generate();
		}
		return meshImprover.getMesh();
	}
}

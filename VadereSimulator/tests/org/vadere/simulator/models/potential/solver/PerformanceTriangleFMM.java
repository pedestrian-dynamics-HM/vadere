package org.vadere.simulator.models.potential.solver;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.EikMesh;
import org.vadere.meshing.utils.tex.TexGraphGenerator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.mesh.PotentialPoint;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.calculators.mesh.EikonalSolverFMMTriangulation;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.PathFindingTag;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class PerformanceTriangleFMM {

	private static Logger log = LogManager.getLogger(TestFFMNonUniformTriangulation.class);
	private static int width;
	private static int height;
	private static VRectangle bbox;
	private static IIncrementalTriangulation<PotentialPoint, PVertex<PotentialPoint>, PHalfEdge<PotentialPoint>, PFace<PotentialPoint>> triangulation;
	private static IDistanceFunction distanceFunc;

	private static EikMesh<PotentialPoint, PVertex<PotentialPoint>, PHalfEdge<PotentialPoint>, PFace<PotentialPoint>> createEikMesh(
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			final double initialEdgeLen) {
		IMeshSupplier<PotentialPoint, PVertex<PotentialPoint>, PHalfEdge<PotentialPoint>, PFace<PotentialPoint>> meshSupplier = () -> new PMesh<>((x, y) -> new PotentialPoint(x, y));
		EikMesh<PotentialPoint, PVertex<PotentialPoint>, PHalfEdge<PotentialPoint>, PFace<PotentialPoint>> eikMesh = new EikMesh<>(
				distanceFunc,
				edgeLengthFunc,
				initialEdgeLen,
				bbox,
				new ArrayList<>(),
				meshSupplier);

		return eikMesh;
	}

	public static void main(String... args) {

		//IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;

		//distanceFunc = p -> -10+Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY());
		//IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.max(Math.abs(p.getX()), Math.abs(p.getY()))) - 3;
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin()*10;
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p));
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin();
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p));


		bbox = new VRectangle(-12, -12, 24, 24);

		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + 0.5*Math.min(Math.abs(distanceFunc.apply(p) + 4), Math.abs(distanceFunc.apply(p)));
		IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p)) * 0.5;
		List<VRectangle> targetAreas = new ArrayList<>();
		List<IPoint> targetPoints = new ArrayList<>();

		/**
		 * We use the pointer based implementation
		 */
		EikMesh<PotentialPoint, PVertex<PotentialPoint>, PHalfEdge<PotentialPoint>, PFace<PotentialPoint>> meshGenerator = createEikMesh(edgeLengthFunc, 0.6);
		// () -> new PMesh<>((x, y) -> new EikMeshPoint(x, y, false))
		meshGenerator.generate();
		triangulation = meshGenerator.getTriangulation();



		Predicate<PFace<PotentialPoint>> nonAccute = f -> triangulation.getMesh().toTriangle(f).isNonAcute();
		//MeshPanel meshPanel = new MeshPanel(meshGenerator.getMesh(), nonAccute, 1000, 1000, bbox);
		//meshPanel.display();

		//targetPoints.add(new MeshPoint(0, 0, false));


		VRectangle rect = new VRectangle(width / 2, height / 2, 100, 100);
		targetAreas.add(rect);

		List<PVertex<PotentialPoint>> targetVertices = triangulation.getMesh().getBoundaryVertices().stream().collect(Collectors.toList());

		EikonalSolver solver = new EikonalSolverFMMTriangulation(
				new UnitTimeCostFunction(),
				triangulation,
				targetVertices,
				distanceFunc);
		long ms = System.currentTimeMillis();
		log.info("start FFM");
		solver.initialize();
		log.info("FFM finished");
		log.info("time: " + (System.currentTimeMillis() - ms));
		double maxError = 0;
		double sum = 0;
		int counter = 0;
		try {
			//System.out.println(getClass().getClassLoader().getResource("./potentialField.csv").getFile());
			Date timestamp = new Date();

			FileWriter potentialFieldWriter = new FileWriter("./output/" + timestamp.getTime() + "potentialField_adapt_0_7.csv");
			FileWriter meshWriter = new FileWriter("./output/"+ timestamp.getTime() + "mesh.tex");
			meshWriter.write(TexGraphGenerator.toTikz(triangulation.getMesh(), true));

			for(double y = bbox.getMinY()+2; y <= bbox.getMaxY()-2; y += 0.1) {
				for(double x = bbox.getMinX()+2; x < bbox.getMaxX()-2; x += 0.1) {
					double val = solver.getPotential(x ,y);
					if(val >= 0.0 && val < Double.MAX_VALUE) {
						double side = Math.min((new VPoint(x, y).distanceToOrigin()-2.0), (10 - new VPoint(x, y).distanceToOrigin()));
						side = Math.max(side, 0.0);
						maxError = Math.max(maxError, Math.abs(val - side));
						sum +=  Math.abs(val - side) *  Math.abs(val - side);
						counter++;
					}
					potentialFieldWriter.write(""+solver.getPotential(x ,y) + " ");
				}
				potentialFieldWriter.write("\n");
			}
			potentialFieldWriter.flush();
			potentialFieldWriter.close();
			meshWriter.flush();
			meshWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info(triangulation.getMesh().getVertices().size());
		log.info("max edge length: " + triangulation.getMesh().streamEdges().map(e -> triangulation.getMesh().toLine(e).length()).max(Comparator.comparingDouble(d -> d)));
		log.info("min edge length: " +triangulation.getMesh().streamEdges().map(e -> triangulation.getMesh().toLine(e).length()).min(Comparator.comparingDouble(d -> d)));

		log.info("max distance to boundary: " + triangulation.getMesh().getBoundaryVertices().stream().map(p -> Math.abs(distanceFunc.apply(p))).max(Comparator.comparingDouble(d -> d)));
		//log.info("L2-Error: " + computeL2Error(triangulation, distanceFunc));
		log.info("max error: " + maxError);
		log.info("max error-2: " + triangulation.getMesh().getVertices().stream().map(p -> Math.abs(Math.abs(p.getPoint().getPotential() + distanceFunc.apply(p)))).max(Comparator.comparingDouble(d -> d)));

		log.info("L2-error: " + Math.sqrt(sum / counter));
		log.info("L2-error-2: " + Math.sqrt(triangulation.getMesh().getVertices().stream()
				.map(p -> Math.abs(Math.abs(p.getPoint().getPotential() + distanceFunc.apply(p))))
				.map(val -> val * val)
				.reduce(0.0, (d1, d2) -> d1 + d2) / triangulation.getMesh().getNumberOfVertices()));
		//assertTrue(0.0 == solver.getValue(5, 5));
		//assertTrue(0.0 < solver.getValue(1, 7));
	}
}

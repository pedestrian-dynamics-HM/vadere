package org.vadere.simulator.models.potential.solver;

import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.vadere.meshing.examples.MeshExamples;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.utils.io.poly.MeshPolyReader;
import org.vadere.meshing.utils.io.poly.MeshPolyWriter;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class TestFMMEikMesh {
	private static Logger log = Logger.getLogger(TestFMMEikMesh.class);

	@Test
	public void testTriangulationFMMMuenchnerFreiheit() throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/muenchner_freiheit.poly");
		MeshPolyReader<PVertex, PHalfEdge, PFace> meshReader = new MeshPolyReader<>(() -> new PMesh());
		var mesh = meshReader.readMesh(inputStream);

		IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation = new IncrementalTriangulation<>(mesh);

		double xmin = 150;
		double ymin = 80;
		double h = 10;
		double w = 10;

		VRectangle targetRectangle = new VRectangle(xmin, ymin, h, w);
		VPoint targetPoint = new VPoint(xmin, ymin);

		EikonalSolver solver = new MeshEikonalSolverFMM(
				new UnitTimeCostFunction(),
				Collections.singleton(targetPoint),
				triangulation);
		long ms = System.currentTimeMillis();
		log.info("start FFM");
		solver.solve();
		log.info("FFM finished");
		log.info("time: " + (System.currentTimeMillis() - ms));

		MeshPolyWriter<PVertex, PHalfEdge, PFace> meshPolyWriter = new MeshPolyWriter<>();

		//System.out.println(meshPolyWriter.to2DPoly(triangulation.getMesh(), 1, i -> "potential", v -> false));

		System.out.println(mesh.toPythonTriangulation(v -> triangulation.getMesh().getDoubleData(v, "potential")));
	}

	@Test
	public void testTriangulationFMMKaiserslautern() throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/kaiserslautern_tri.poly");
		MeshPolyReader<PVertex, PHalfEdge, PFace> meshReader = new MeshPolyReader<>(() -> new PMesh());
		var mesh = meshReader.readMesh(inputStream);

		IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation = new IncrementalTriangulation<>(mesh);

		double xmin = 150;
		double ymin = 80;
		double h = 10;
		double w = 10;

		VRectangle targetRectangle = new VRectangle(xmin, ymin, h, w);
		VPoint targetPoint = new VPoint(40, 40);

		EikonalSolver solver = new MeshEikonalSolverFMM(
				new UnitTimeCostFunction(),
				Collections.singleton(targetPoint),
				triangulation);
		long ms = System.currentTimeMillis();
		log.info("start FFM");
		solver.solve();
		log.info("FFM finished");
		log.info("time: " + (System.currentTimeMillis() - ms));



		ms = System.currentTimeMillis();
		log.info("start walk");
		solver.getPotential(10, 10, this);
		log.info("walk finished");
		log.info("time: " + (System.currentTimeMillis() - ms));

		ms = System.currentTimeMillis();
		log.info("start cached walk");
		solver.getPotential(10, 10, this);
		log.info("walk finished");
		log.info("time: " + (System.currentTimeMillis() - ms));

		MeshPolyWriter<PVertex, PHalfEdge, PFace> meshPolyWriter = new MeshPolyWriter<>();

		//System.out.println(meshPolyWriter.to2DPoly(triangulation.getMesh(), 1, i -> "potential", v -> false));

		System.out.println(mesh.toPythonTriangulation(v -> triangulation.getMesh().getDoubleData(v, "potential")));
	}

	@Ignore
	@Test
	public void testFilledChickenFMM() throws IOException {
		testTriangulationFMM("/poly/filled_chicken.poly", new VPoint(2,2), 3.0);
	}

	@Ignore
	@Test
	public void testBridge() throws IOException {
		testTriangulationFMM("/poly/bridge.poly", new VPoint(42.0, 46.0), 2.0);
	}

	private void testTriangulationFMM(@NotNull final String file, @NotNull final VPoint targetPoint, final double h0) throws IOException {
		// 1. read the base PSLG-file
		final InputStream inputStream = MeshExamples.class.getResourceAsStream(file);
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);

		// 2. generate the unstructured mesh using EikMesh
		Collection<VPoint> fixPoints = new ArrayList<>();
		fixPoints.add(targetPoint);

		var eikMesh = new GenEikMesh<>(
				IDistanceFunction.create(pslg.getSegmentBound(), pslg.getHoles()),
				p -> h0,
				fixPoints,
				h0,
				pslg.getBoundingBox(),
				pslg.getAllPolygons(),
				() -> new PMesh());

		PMeshPanel panel = new PMeshPanel(eikMesh.getMesh(), 600, 800);
		panel.display();

		// long for: eikMesh.generate();
		while (!eikMesh.isFinished()) {
			eikMesh.improve();
			panel.repaint();
		}
		//eikMesh.generate();

		// 3. solve the eikonal equation on the given mehsh
		EikonalSolver solver = new MeshEikonalSolverFMM(
				new UnitTimeCostFunction(),
				eikMesh.getTriangulation(),
				eikMesh.getFixVertices().stream().filter(v -> v.distance(targetPoint) < GeometryUtils.DOUBLE_EPS).collect(Collectors.toList()));
		long ms = System.currentTimeMillis();
		log.info("start FFM");
		solver.solve();
		log.info("FFM finished");
		log.info("time: " + (System.currentTimeMillis() - ms));

		// 4. print the result to the console i.e. standard out
		MeshPolyWriter<PVertex, PHalfEdge, PFace> meshPolyWriter = new MeshPolyWriter<>();
		System.out.println(meshPolyWriter.to2DPoly(eikMesh.getMesh(), 1, i -> "potential", v -> false));

		//System.out.println(eikMesh.getMesh().toPythonTriangulation(v -> eikMesh.getMesh().getDoubleData(v, "potential")));
	}
}

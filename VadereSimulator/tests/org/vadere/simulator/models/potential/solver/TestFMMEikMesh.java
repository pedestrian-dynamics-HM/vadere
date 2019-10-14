package org.vadere.simulator.models.potential.solver;

import org.junit.Ignore;
import org.junit.Test;
import org.vadere.meshing.examples.MeshExamples;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.utils.io.poly.MeshPolyReader;
import org.vadere.meshing.utils.io.poly.MeshPolyWriter;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.mesh.EikonalSolverFMMTriangulation;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

public class TestFMMEikMesh {
	private static Logger log = Logger.getLogger(TestFMMEikMesh.class);

	@Test
	public void testTriangulationFMM() throws IOException {
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

		EikonalSolver solver = new EikonalSolverFMMTriangulation(
				new UnitTimeCostFunction(),
				Collections.singleton(targetPoint),
				triangulation);
		long ms = System.currentTimeMillis();
		log.info("start FFM");
		solver.initialize();
		log.info("FFM finished");
		log.info("time: " + (System.currentTimeMillis() - ms));

		MeshPolyWriter<PVertex, PHalfEdge, PFace> meshPolyWriter = new MeshPolyWriter<>();

		System.out.println(meshPolyWriter.to2DPoly(triangulation.getMesh(), 1, i -> "potential", v -> false));

		//System.out.println(mesh.toPythonTriangulation(v -> triangulation.getMesh().getDoubleData(v, "potential")));
	}
}

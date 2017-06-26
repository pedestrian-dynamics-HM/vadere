package org.vadere.util.potential;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.potential.calculators.EikonalSolver;
import org.vadere.util.potential.calculators.EikonalSolverFMMAcuteTriangulation;
import org.vadere.util.potential.calculators.EikonalSolverFMMTriangulation;
import org.vadere.util.potential.calculators.PotentialPoint;
import org.vadere.util.geometry.mesh.gen.UniformTriangulation;
import org.vadere.util.potential.timecost.UnitTimeCostFunction;
import org.vadere.util.triangulation.IPointConstructor;
import org.vadere.util.triangulation.adaptive.MeshPoint;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TestFFMUniformTriangulation {

	private static Logger log = LogManager.getLogger(TestFFMUniformTriangulation.class);

	private UniformTriangulation<PotentialPoint, PVertex<PotentialPoint>, PHalfEdge<PotentialPoint>, PFace<PotentialPoint>> uniformTriangulation;
	private int width = 10;
	private int height = 10;
	private double minTriangleSideLength = 0.1;

	@Before
	public void setUp() throws Exception {
		IPointConstructor<PotentialPoint> pointConstructor = (x, y) -> new MeshPoint(x, y, false);
		uniformTriangulation = ITriangulation.createUniformTriangulation(
				IPointLocator.Type.BASE,
				new VRectangle(0, 0, width, height),
				minTriangleSideLength,
				pointConstructor
				);
		uniformTriangulation.compute();
		uniformTriangulation.finalize();
	}

	@Test
	public void testFFM() {
		List<IPoint> targetPoints = new ArrayList<>();
		targetPoints.add(new MeshPoint(5,5, false));
		//EikonalSolver solver = new EikonalSolverFMMAcuteTriangulation(targetPoints, new UnitTimeCostFunction(), uniformTriangulation);

		EikonalSolver solver = new EikonalSolverFMMTriangulation(targetPoints, new UnitTimeCostFunction(), uniformTriangulation);
		log.info("start FFM");
		solver.initialize();
		log.info("FFM finished");
		try {
			//System.out.println(getClass().getClassLoader().getResource("./potentialField.csv").getFile());
			FileWriter writer = new FileWriter("./potentialField.csv");
			for(double y = 0.2; y < height-0.2; y += 0.1) {
				for(double x = 0.2; x < width-0.2; x += 0.1) {
					writer.write(""+solver.getValue(x ,y) + " ");
				}
				writer.write("\n");
			}
			writer.flush();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		assertTrue(0.1 > solver.getValue(5, 5));
		assertTrue(0.0 < solver.getValue(1, 7));
	}
}

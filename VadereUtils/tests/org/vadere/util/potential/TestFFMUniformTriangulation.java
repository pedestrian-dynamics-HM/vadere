package org.vadere.util.potential;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.data.HalfEdge;
import org.vadere.util.potential.calculators.EikonalSolver;
import org.vadere.util.potential.calculators.EikonalSolverFMMTriangulation;
import org.vadere.util.potential.calculators.PotentialPoint;
import org.vadere.util.potential.timecost.UnitTimeCostFunction;
import org.vadere.util.triangulation.UniformTriangulation;
import org.vadere.util.triangulation.adaptive.MeshPoint;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TestFFMUniformTriangulation {

	private static Logger log = LogManager.getLogger(TestFFMUniformTriangulation.class);

	private UniformTriangulation<PotentialPoint> uniformTriangulation;
	private int width = 100;
	private int height = 100;
	private double minTriangleSideLength = 10.0;

	@Before
	public void setUp() throws Exception {
		uniformTriangulation = new UniformTriangulation<>(
				0,
				0,
				width,
				height,
				minTriangleSideLength,
				(x, y) -> new MeshPoint(x, y, false));
		uniformTriangulation.compute();
	}

	@Test
	public void testFFM() {
		uniformTriangulation.finalize();
		List<HalfEdge<PotentialPoint>> targetPoint = uniformTriangulation.locate(0.1, 0.1).getEdges();
		EikonalSolver solver = new EikonalSolverFMMTriangulation(targetPoint, new UnitTimeCostFunction(), uniformTriangulation);
		log.info("start FFM");
		solver.initialize();
		log.info("FFM finished");
		try {
			//System.out.println(getClass().getClassLoader().getResource("./potentialField.csv").getFile());
			FileWriter writer = new FileWriter("./potentialField.csv");
			for(double y = 0.1; y < height; y += 1.0) {
				for(double x = 0.1; x < width; x += 1.0) {
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

//		assertTrue(0.0 == solver.getValue(0.1, 0.1));
//		assertTrue(0.0 > solver.getValue(15.5, 10.3));
	}

}

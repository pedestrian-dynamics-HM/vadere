package org.vadere.util.potential;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.data.Triangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.calculators.EikonalSolver;
import org.vadere.util.potential.calculators.EikonalSolverFMMAcuteTriangulation;
import org.vadere.util.potential.calculators.EikonalSolverFMMTriangulation;
import org.vadere.util.potential.calculators.PotentialPoint;
import org.vadere.util.potential.timecost.UnitTimeCostFunction;
import org.vadere.util.triangulation.adaptive.PSDistmesh;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TestFFMNonUniformTriangulation {

	private static Logger log = LogManager.getLogger(TestFFMNonUniformTriangulation.class);
	private int width;
	private int height;
	private Triangulation<? extends PotentialPoint> triangulation;

	@Before
	public void setUp() {
		double h0 = 1.0;
		width = 300;
		height = 300;
		VRectangle bbox = new VRectangle(0, 0, width, height);
		/*List<VShape> obstacles = new ArrayList<VShape>() {{
			add(new VRectangle(0.65*300, -5, 0.1*300, 0.6*300));
			add(new VRectangle(0.65*300, 0.7*300, 0.1*300, 0.3*300));
		}};*/

		List<VShape> boundingBox = new ArrayList<VShape>() {{
			add(new VRectangle(0, 0, 5, 300));
			add(new VRectangle(0, 0, 300, 5));
			add(new VRectangle(295, 0, 5, 300));
			add(new VRectangle(0, 295, 5, 300));
		}};

		PSDistmesh meshGenerator = new PSDistmesh(bbox, boundingBox, h0,false);
		meshGenerator.execute();
		meshGenerator.cleanUp();
		triangulation = meshGenerator.getTriangulation();
	}

	@Test
	public void testFMM() {
		List<IPoint> targetPoints = new ArrayList<>();
		targetPoints.add(new VPoint(width/2,height/2));

		EikonalSolver solver = new EikonalSolverFMMTriangulation(targetPoints, new UnitTimeCostFunction(), triangulation);

		log.info("start FFM");
		solver.initialize();
		log.info("FFM finished");
		try {
			//System.out.println(getClass().getClassLoader().getResource("./potentialField.csv").getFile());
			FileWriter writer = new FileWriter("./potentialField.csv");
			for(double y = 0.2; y < height-0.2; y += 1.0) {
				for(double x = 0.2; x < width-0.2; x += 1.0) {
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

		//assertTrue(0.0 == solver.getValue(5, 5));
		//assertTrue(0.0 < solver.getValue(1, 7));
	}
}

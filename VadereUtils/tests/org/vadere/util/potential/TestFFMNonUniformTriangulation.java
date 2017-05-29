package org.vadere.util.potential;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.mesh.impl.PFace;
import org.vadere.util.geometry.mesh.impl.PHalfEdge;
import org.vadere.util.geometry.mesh.impl.PVertex;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.calculators.EikonalSolver;
import org.vadere.util.potential.calculators.EikonalSolverFMMTriangulation;
import org.vadere.util.potential.timecost.UnitTimeCostFunction;
import org.vadere.util.geometry.mesh.triangulations.IncrementalTriangulation;
import org.vadere.util.triangulation.adaptive.MeshPoint;
import org.vadere.util.triangulation.adaptive.PSDistmesh;
import org.vadere.util.triangulation.adaptive.PSDistmeshPanel;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;


public class TestFFMNonUniformTriangulation {

	private static Logger log = LogManager.getLogger(TestFFMNonUniformTriangulation.class);
	private int width;
	private int height;
	private ITriangulation<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> triangulation;

	@Before
	public void setUp() {
		double h0 = 5.0;
		width = 300;
		height = 300;
		VRectangle bbox = new VRectangle(0, 0, width, height);
		/*List<VShape> obstacles = new ArrayList<VShape>() {{
			add(new VRectangle(0.65*300, -5, 0.1*300, 0.6*300));
			add(new VRectangle(0.65*300, 0.7*300, 0.1*300, 0.3*300));
		}};*/

		List<VShape> boundingBox = new ArrayList<VShape>() {{
			add(new VRectangle(0, 0, 5, width));
			add(new VRectangle(0, 0, width, 5));
			add(new VRectangle(width-5, 0, 5, height));
			add(new VRectangle(0, height-5, 5, height));
		}};

		PSDistmesh meshGenerator = new PSDistmesh(bbox, boundingBox, h0,false);
		meshGenerator.execute();
		triangulation = meshGenerator.getTriangulation();

		PSDistmeshPanel distmeshPanel = new PSDistmeshPanel(meshGenerator, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);

		//triangulation = new UniformTriangulation<>(0, 0, width, height, 10.0, (a,b) -> new MeshPoint(a,b, false));
		//triangulation.finalize();
	}

	@Test
	public void testFMM() {
		List<VRectangle> targetAreas = new ArrayList<>();
		List<IPoint> targetPoints = new ArrayList<>();
		targetPoints.add(new MeshPoint(40, 40, false));
		VRectangle rect = new VRectangle(width / 2, height / 2, 100, 100);
		targetAreas.add(rect);

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

		while (true) {}

		//assertTrue(0.0 == solver.getValue(5, 5));
		//assertTrue(0.0 < solver.getValue(1, 7));
	}
}

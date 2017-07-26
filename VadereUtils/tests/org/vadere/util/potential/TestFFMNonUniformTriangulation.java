package org.vadere.util.potential;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.calculators.EikonalSolver;
import org.vadere.util.potential.calculators.EikonalSolverFMM;
import org.vadere.util.potential.calculators.EikonalSolverFMMTriangulation;
import org.vadere.util.potential.timecost.ITimeCostFunction;
import org.vadere.util.potential.timecost.UnitTimeCostFunction;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;
import org.vadere.util.triangulation.adaptive.MeshPoint;
import org.vadere.util.triangulation.adaptive.PSMeshing;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class TestFFMNonUniformTriangulation {

	private static Logger log = LogManager.getLogger(TestFFMNonUniformTriangulation.class);
	private int width;
	private int height;
	private VRectangle bbox;
	private ITriangulation<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> triangulation;
	private IDistanceFunction distanceFunc;

	@Before
	public void setUp() {
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
	}

	@Test
	public void testTriangulationFMM() {

		IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.min(Math.abs(distanceFunc.apply(p) + 4), Math.abs(distanceFunc.apply(p)));
		IEdgeLengthFunction unifromEdgeLengthFunc = p -> 1.0;
		List<VRectangle> targetAreas = new ArrayList<>();
		List<IPoint> targetPoints = new ArrayList<>();
		PSMeshing meshGenerator = new PSMeshing(distanceFunc, edgeLengthFunc, 0.6, bbox, new ArrayList<>());
		meshGenerator.execute();
		triangulation = meshGenerator.getTriangulation();

		//targetPoints.add(new MeshPoint(0, 0, false));


		VRectangle rect = new VRectangle(width / 2, height / 2, 100, 100);
		targetAreas.add(rect);

		EikonalSolver solver = new EikonalSolverFMMTriangulation(new UnitTimeCostFunction(), triangulation,
				triangulation.getMesh().getBoundaryVertices().stream().collect(Collectors.toList()));

		log.info("start FFM");
		solver.initialize();
		log.info("FFM finished");
		double maxError = 0;
		try {
			//System.out.println(getClass().getClassLoader().getResource("./potentialField.csv").getFile());
			FileWriter writer = new FileWriter("./potentialField.csv");
			for(double y = bbox.getMinY()+2; y <= bbox.getMaxY()-2; y += 0.1) {
				for(double x = bbox.getMinX()+2; x < bbox.getMaxX()-2; x += 0.1) {
					double val = solver.getValue(x ,y);
					if(val >= 0.0) {
						double side = Math.min((new VPoint(x, y).distanceToOrigin()-2.0), (10 - new VPoint(x, y).distanceToOrigin()));
						side = Math.max(side, 0.0);
						maxError = Math.max(maxError, Math.abs(val - side));
					}
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

		log.info(triangulation.getMesh().getVertices().size());
		log.info("max edge length: " + triangulation.getMesh().streamEdges().map(e -> triangulation.getMesh().toLine(e).length()).max((d1, d2) -> Double.compare(d1, d2)));
		log.info("min edge length: " +triangulation.getMesh().streamEdges().map(e -> triangulation.getMesh().toLine(e).length()).min((d1, d2) -> Double.compare(d1, d2)));
		log.info("max error: " + maxError);
		//assertTrue(0.0 == solver.getValue(5, 5));
		//assertTrue(0.0 < solver.getValue(1, 7));
	}

	@Test
	public void testRegularFMM() {

		double resolution = 0.4;
		double rad = 2.0;
		CellGrid cellGrid = new CellGrid(bbox.getWidth()-4, bbox.getHeight()-4,resolution, new CellState());
		cellGrid.pointStream()
				.filter(p -> new VPoint(p.getX() * resolution, p.getY() * resolution).distance(new VPoint(10, 10)) <= rad
						|| new VPoint(p.getX() * resolution, p.getY() * resolution).distance(new VPoint(10, 10)) >= 10)
				.forEach(p -> cellGrid.setValue(p, new CellState(0.0, PathFindingTag.Target)));

		EikonalSolver solver = new EikonalSolverFMM(cellGrid, new ArrayList<>(), false, new UnitTimeCostFunction(), 0.1);


		log.info("start FFM");
		solver.initialize();
		log.info("FFM finished");
		double maxError = 0;
		try {
			//System.out.println(getClass().getClassLoader().getResource("./potentialField.csv").getFile());
			FileWriter writer = new FileWriter("./potentialField_reg.csv");
			for(double y = 0; y <= bbox.getHeight()-4; y += 0.1) {
				for(double x = 0; x < bbox.getWidth()-4; x += 0.1) {
					double val = solver.getValue(x ,y);
					if(val >= 0.0) {
						double side = Math.min(new VPoint(x, y).distance(new VPoint(10, 10))-2.0, 10 - new VPoint(x, y).distance(new VPoint(10, 10)));
						side = Math.max(side, 0.0);
						//double distance = (new VPoint(x, y).distance(new VPoint(10, 10)))-rad;
						//maxError = Math.max(maxError, Math.abs((distance >= 0.0 ? val-distance : val)));
						maxError = Math.max(maxError, Math.abs(val - side));
					}
					writer.write(""+solver.getValue(x,y) + " ");
				}
				writer.write("\n");
			}
			writer.flush();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("max error: " + maxError);
	}
}

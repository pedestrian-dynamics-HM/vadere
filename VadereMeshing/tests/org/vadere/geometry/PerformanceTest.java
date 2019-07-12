package org.vadere.geometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;

import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.voronoi.VoronoiDiagram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author Benedikt Zoennchen
 */
public class PerformanceTest {

	private static Logger log = Logger.getLogger(TestBoyerWatson.class);

	private static List<VPoint> points;

	private static int width = 300;

	private static int height = 300;

	public static void main(String... args) {
		testPerformanceForDifferentPointLocators();
	}

	private static void setUp() {
		points = new ArrayList<>();

		Random r = new Random(0);
		int numberOfPoints = 1_000_000;

		for(int i=0; i< numberOfPoints; i++) {
			VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
			points.add(point);
		}
	}

	private static void testPointerWalk() {
		long ms = System.currentTimeMillis();
		IIncrementalTriangulation<PVertex, PHalfEdge, PFace> delaunay = IIncrementalTriangulation.createPTriangulation(IPointLocator.Type.BASE, points);
		delaunay.finish();
		log.info("runtime of the Walk method, #vertices = " + delaunay.getVertices().size() + " is " + (System.currentTimeMillis() - ms) + " [ms]");
	}

	private static void testArrayJumpAndWalk() {
		long ms = System.currentTimeMillis();
		IIncrementalTriangulation<AVertex, AHalfEdge, AFace> delaunay = IIncrementalTriangulation.createATriangulation(IPointLocator.Type.JUMP_AND_WALK, points);
		delaunay.finish();
		log.info("runtime of the Jump & Walk method (Array), #vertices = " + delaunay.getVertices().size() + " is " + (System.currentTimeMillis() - ms) + " [ms]");
	}

	private static void testPointerJumpAndWalk() {
		long ms = System.currentTimeMillis();
		IIncrementalTriangulation<PVertex, PHalfEdge, PFace> delaunay = IIncrementalTriangulation.createPTriangulation(IPointLocator.Type.JUMP_AND_WALK, points);
		delaunay.finish();
		log.info("runtime of the Jump & Walk method (Pointer), #vertices = " + delaunay.getVertices().size() + " is " + (System.currentTimeMillis() - ms) + " [ms]");
	}

	private static void testPointerDelaunayHierarchy() {
		long ms = System.currentTimeMillis();
		IIncrementalTriangulation<PVertex, PHalfEdge, PFace> delaunay = IIncrementalTriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, points);
		delaunay.finish();
		log.info("runtime of the Delaunay-Hierarchy (Pointer), #vertices = " + delaunay.getVertices().size() + " is " + (System.currentTimeMillis() - ms) + " [ms]");
	}

	private static void testArrayDelaunayHierarchy() {
		long ms = System.currentTimeMillis();
		IIncrementalTriangulation<AVertex, AHalfEdge, AFace> delaunay = IIncrementalTriangulation.createATriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, points);
		delaunay.finish();
		log.info("runtime of the Delaunay-Hierarchy (Array), #vertices = " + delaunay.getVertices().size() + " is " + (System.currentTimeMillis() - ms) + " [ms]");
	}

	private static void testSweepline() {
		VoronoiDiagram voronoiDiagram = new VoronoiDiagram(new VRectangle(0,0, width, height));
		double ms = System.currentTimeMillis();
		voronoiDiagram.computeVoronoiDiagram(points);
		log.info("runtime of the Sweepline method, #vertices = " + points.size() + " is " + (System.currentTimeMillis() - ms) + " [ms]");
	}

	private static void testJTSDelaunayBuilder() {
		GeometryFactory fact = new GeometryFactory();
		Collection<Coordinate> coords = points.stream().map(p -> new Coordinate(p.getX(), p.getY())).collect(Collectors.toList());
		double ms = System.currentTimeMillis();
		DelaunayTriangulationBuilder builder = new DelaunayTriangulationBuilder();
		builder.setSites(coords);
		builder.getTriangles(fact);
		log.info("runtime of the Quad-Tree method (JTS), #vertices = " + points.size() + " is " + (System.currentTimeMillis() - ms) + " [ms]");
	}

	public static void testPerformanceForDifferentPointLocators() {
		setUp();
		testSweepline();
		testJTSDelaunayBuilder();
		//testPointerDelaunayHierarchy();
		//testArrayDelaunayHierarchy();
		testArrayJumpAndWalk();
		testPointerJumpAndWalk();
		//testPointerWalk();
	}

}

package org.vadere.util.triangulation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AHalfEdge;
import org.vadere.util.geometry.mesh.gen.AVertex;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
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

	private static Logger log = LogManager.getLogger(TestBoyerWatson.class);

	private static List<VPoint> points;

	private static int width = 300;

	private static int height = 300;

	public static void main(String... args) {
		testPerformanceForDifferentPointLocators();
	}

	private static void setUp() {
		points = new ArrayList<>();

		Random r = new Random();
		int numberOfPoints = 300000;

		for(int i=0; i< numberOfPoints; i++) {
			VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
			points.add(point);
		}
	}

	private static void testPointerWalk() {
		long ms = System.currentTimeMillis();
		ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> delaunay = ITriangulation.createPTriangulation(IPointLocator.Type.BASE, points, (x, y) -> new VPoint(x, y));
		delaunay.finish();
		log.info("runtime of the Walk method, #vertices = " + delaunay.getVertices().size() + " is " + (System.currentTimeMillis() - ms) + " [ms]");
	}

	private static void testPointerJumpAndWalk() {
		long ms = System.currentTimeMillis();
		ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> delaunay = ITriangulation.createPTriangulation(IPointLocator.Type.JUMP_AND_WALK, points, (x, y) -> new VPoint(x, y));
		delaunay.finish();
		log.info("runtime of the Jump & Walk method, #vertices = " + delaunay.getVertices().size() + " is " + (System.currentTimeMillis() - ms) + " [ms]");
	}

	private static void testPointerDelaunayHierarchy() {
		long ms = System.currentTimeMillis();
		ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> delaunay = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, points, (x, y) -> new VPoint(x, y));
		delaunay.finish();
		log.info("runtime of the Delaunay-Hierarchy (Pointer), #vertices = " + delaunay.getVertices().size() + " is " + (System.currentTimeMillis() - ms) + " [ms]");
	}

	private static void testArrayDelaunayHierarchy() {
		long ms = System.currentTimeMillis();
		ITriangulation<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> delaunay = ITriangulation.createATriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, points, (x, y) -> new VPoint(x, y));
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
		testPointerDelaunayHierarchy();
		testPointerJumpAndWalk();
		testPointerWalk();
	}

}

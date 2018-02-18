package org.vadere.util.triangulation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.gen.*;
import org.vadere.util.geometry.mesh.impl.VPTriangulation;
import org.vadere.util.geometry.mesh.inter.*;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.io.CollectionUtils;
import org.vadere.util.voronoi.VoronoiDiagram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class TestBoyerWatson {

	private static Logger log = LogManager.getLogger(TestBoyerWatson.class);

	@Before
	public void setUp() throws Exception {}

	@Test
	public void testDifferentPointLocator() {
		VPoint p1 = new VPoint(0, 0);
		VPoint p2 = new VPoint(50, 0);
		VPoint p3 = new VPoint(50, 50);
		VPoint p4 = new VPoint(0, 50);

		VPoint p6 = new VPoint(50, 50);
		VPoint p5 = new VPoint(25, 25);

		Set<VPoint> points = new HashSet<>();
		points.add(p1);
		points.add(p2);
		points.add(p3);
		points.add(p4);
		points.add(p6);
		points.add(p5);


		List<ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>>> triangulationList = new ArrayList<>();

		triangulationList.add(ITriangulation.createPTriangulation(IPointLocator.Type.BASE, points, (x, y) -> new VPoint(x, y)));
		triangulationList.add(ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_TREE, points, (x, y) -> new VPoint(x, y)));
		triangulationList.add(ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, points, (x, y) -> new VPoint(x, y)));

		for(ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> delaunayTriangulation : triangulationList) {
			delaunayTriangulation.finish();
			Set<VTriangle> triangulation = delaunayTriangulation.streamTriangles().collect(Collectors.toSet());

			Set<VPoint> triangle1 = new HashSet<>();
			triangle1.add(p1);
			triangle1.add(p5);
			triangle1.add(p4);

			Set<VPoint> triangle2 = new HashSet<>();
			triangle2.add(p1);
			triangle2.add(p2);
			triangle2.add(p5);

			Set<VPoint> triangle3 = new HashSet<>();
			triangle3.add(p2);
			triangle3.add(p3);
			triangle3.add(p5);

			Set<VPoint> triangle4 = new HashSet<>();
			triangle4.add(p4);
			triangle4.add(p5);
			triangle4.add(p3);

			Set<Set<VPoint>> pointSets = triangulation.stream().map(t -> new HashSet<>(t.getPoints())).collect(Collectors.toSet());

			Set<Set<VPoint>> expextedPointSets = new HashSet<>();
			expextedPointSets.add(triangle1);
			expextedPointSets.add(triangle2);
			expextedPointSets.add(triangle3);
			expextedPointSets.add(triangle4);

			assertTrue(expextedPointSets.equals(pointSets));

			triangulation.forEach(log::info);
		}
	}

	@Test
	public void testSplitTriangle() {

		VPoint p1 = new VPoint(0, 0);
		VPoint p2 = new VPoint(50, 0);
		VPoint p3 = new VPoint(25, 25);
		VPoint centerPoint = new VPoint(25, 10);

		Set<VPoint> points = new HashSet<>();
		points.add(p1);
		points.add(p2);
		points.add(p3);

		ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> delaunayTriangulation = ITriangulation.createPTriangulation(IPointLocator.Type.BASE, points, (x, y) -> new VPoint(x, y));
		PFace<VPoint> face = delaunayTriangulation.locateFace(centerPoint).get();
		IMesh<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> mesh = delaunayTriangulation.getMesh();
		PHalfEdge<VPoint> edge = mesh.getEdge(face);

		// triangulations are always ccw ordered!
		assertTrue(GeometryUtils.isCCW(mesh.getVertex(edge), mesh.getVertex(mesh.getNext(edge)), mesh.getVertex(mesh.getPrev(edge))));

		delaunayTriangulation.splitTriangle(face, centerPoint, false);
		delaunayTriangulation.finish();

		Set<VTriangle> triangles = delaunayTriangulation.streamTriangles().collect(Collectors.toSet());
		Set<VTriangle> expectedResult = new HashSet<>(Arrays.asList(new VTriangle(p1, p2, centerPoint), new VTriangle(p2, p3, centerPoint), new VTriangle(p1, p3, centerPoint)));

		assertTrue(CollectionUtils.isEqualCollection(triangles, expectedResult, new VTriangleEquator()));
	}

	private class VTriangleEquator implements CollectionUtils.IEquator<VTriangle> {

		@Override
		public boolean equate(final VTriangle a, final VTriangle b) {
			List<VPoint> aPoints = a.getPoints();
			List<VPoint> bPoints = b.getPoints();
			return CollectionUtils.isEqualCollection(aPoints, bPoints, new VPointEquator());
		}

		@Override
		public int hash(VTriangle a) {
			return 0;
		}
	}

	private class VPointEquator implements CollectionUtils.IEquator<VPoint> {

		@Override
		public boolean equate(final VPoint a, final VPoint b) {
			return a.equals(b);
		}

		@Override
		public int hash(VPoint a) {
			return a.hashCode();
		}
	}


	@Test
	public void testPerformanceForDifferentPointLocators() {
		List<VPoint> points = new ArrayList<>();
		int width = 300;
		int height = 300;
		Random r = new Random();
		assert false;
		int numberOfPoints = 100000;

		for(int i=0; i< numberOfPoints; i++) {
			VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
			points.add(point);
		}

		/*Collections.sort(points, (p1, p2) -> {
			if(p1.getX() > p2.getX()){
				return -1;
			}
			else {
				return 1;
			}
		});*/




		long ms = System.currentTimeMillis();
		ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> pdelaunayTriangulation = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, points, (x, y) -> new VPoint(x, y));
        pdelaunayTriangulation.finish();
		log.info("runtime of the BowyerWatson for " + numberOfPoints + " vertices =" + (System.currentTimeMillis() - ms) + " using the delaunay-hierarchy and a pointer-based data structure");

        ms = System.currentTimeMillis();
        ITriangulation<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> adelaunayTriangulation = ITriangulation.createATriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, points, (x, y) -> new VPoint(x, y));
        adelaunayTriangulation.finish();
        log.info("runtime of the BowyerWatson for " + numberOfPoints + " vertices =" + (System.currentTimeMillis() - ms) + " using the delaunay-hierarchy and a array-based data structure");

		VoronoiDiagram voronoiDiagram = new VoronoiDiagram(new VRectangle(0,0,width, height));
		ms = System.currentTimeMillis();
		voronoiDiagram.computeVoronoiDiagram(points);
		log.info("runtime of the Sweepline for " + numberOfPoints + " vertices =" + (System.currentTimeMillis() - ms) + " using the vadere-voronoi");


		log.info("start checking the delaunay property, this can take some time");
		/*Collection<VTriangle> triangles = delaunayTriangulation.streamTriangles().collect(Collectors.toList());

		for(VTriangle triangle : triangles) {

			List<VPoint> trianglePoints = triangle.getPoints();

			for(VTriangle t : triangles) {
				assertTrue(t.getPoints().stream().noneMatch(p -> !trianglePoints.contains(p) && triangle.isInCircumscribedCycle(p)));
			}
		}
		log.info("end checking the delaunay property");*/

		log.info("check vertex adjustment");
		/*final IMesh<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> mesh1 = delaunayTriangulation.getMesh();
		mesh1.streamEdges().forEach(edge -> assertEquals(mesh1.getVertex(edge), mesh1.getVertex(mesh1.getEdge(mesh1.getVertex(edge)))));*/

		ms = System.currentTimeMillis();
		//ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> delaunayTriangulation = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, points, (x, y) -> new VPoint(x, y));
		//delaunayTriangulation.finish();
		log.info("runtime of the BowyerWatson for " + numberOfPoints + " vertices =" + (System.currentTimeMillis() - ms) + " using the delaunay-hierarchy");

		/*log.info("start checking the delaunay property, this can take some time");
		List<VTriangle> triangles = delaunayTriangulation.streamTriangles().collect(Collectors.toList());

		for(VTriangle triangle : triangles) {

			List<VPoint> trianglePoints = triangle.getPoints();

			for(VTriangle t : triangles) {
				assertTrue(t.getPoints().stream().noneMatch(p -> !trianglePoints.contains(p) &&
						triangle.isInCircumscribedCycle(p)));
			}
		}
		log.info("end checking the delaunay property");*/

		log.info("check vertex adjustment");
		/*final IMesh<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> mesh2 = delaunayTriangulation.getMesh();
		mesh2.streamEdges().forEach(edge -> assertEquals(mesh2.getVertex(edge), mesh2.getVertex(mesh2.getEdge(mesh2.getVertex(edge)))));*/

		GeometryFactory fact = new GeometryFactory();
		Collection<Coordinate> coords = points.stream().map(p -> new Coordinate(p.getX(), p.getY())).collect(Collectors.toList());
		ms = System.currentTimeMillis();
		DelaunayTriangulationBuilder builder = new DelaunayTriangulationBuilder();
		builder.setSites(coords);
		builder.getTriangles(fact);
		log.info("runtime of the ? for " + numberOfPoints + " vertices =" + (System.currentTimeMillis() - ms) + " using the JTS-Delaunay-Triangulation");
	}

	@Test
	public void testEdgeVertexRelation() {
		List<VPoint> points = new ArrayList<>();
		int width = 300;
		int height = 300;
		Random r = new Random();
		assert false;
		int numberOfPoints = 5000;

		for (int i = 0; i < numberOfPoints; i++) {
			VPoint point = new VPoint(width * r.nextDouble(), height * r.nextDouble());
			points.add(point);
		}

		VPTriangulation vpTriangulation = ITriangulation.createVPTriangulation(new VRectangle(0, 0, width, height));
		vpTriangulation.insert(points);
		vpTriangulation.finish();

		assertTrue(hasCorrectEdgeVertexRealtion(vpTriangulation));
	}


	private static boolean testTriangulationEquality(final Set<VTriangle> triangulation1, final Set<VTriangle> triangulation2) {
		if(triangulation1.size() != triangulation2.size())
			return false;

		for (VTriangle triangle1 : triangulation1) {
			boolean found = false;
			for (VTriangle triangle2 : triangulation2) {
				if(TestBoyerWatson.testTriangleEquality(triangle1, triangle2)){
					found = true;
				}
			}
			if(!found)
				return false;
		}

		return true;
	}

	private static <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> boolean hasCorrectEdgeVertexRealtion(final ITriangulation<P, V, E, F> triangulation) {
		return triangulation.getMesh()
				.streamVertices()
				.filter(v -> triangulation.getMesh().isAlive(v))
				.noneMatch(v -> triangulation.getMesh().getVertex(triangulation.getMesh().getEdge(v)) != v);
	}

	private static boolean testTriangleEquality(final VTriangle triangle1, final VTriangle triangle2) {
		return	(triangle2.p1.equals(triangle1.p1) && triangle2.p2.equals(triangle1.p2) && triangle2.p3.equals(triangle1.p3)) ||
				(triangle2.p1.equals(triangle1.p1) && triangle2.p2.equals(triangle1.p3) && triangle2.p3.equals(triangle1.p2)) ||
				(triangle2.p1.equals(triangle1.p2) && triangle2.p2.equals(triangle1.p1) && triangle2.p3.equals(triangle1.p3)) ||
				(triangle2.p1.equals(triangle1.p2) && triangle2.p2.equals(triangle1.p3) && triangle2.p3.equals(triangle1.p1)) ||
				(triangle2.p1.equals(triangle1.p3) && triangle2.p2.equals(triangle1.p2) && triangle2.p3.equals(triangle1.p1)) ||
				(triangle2.p1.equals(triangle1.p3) && triangle2.p2.equals(triangle1.p1) && triangle2.p3.equals(triangle1.p2));
	}
}

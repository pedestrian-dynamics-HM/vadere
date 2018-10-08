package org.vadere.util.geometry.mesh.triangulation;

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

import java.util.ArrayList;
import java.util.Arrays;
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
			Set<VPoint> aPoints = new HashSet<>(a.getPoints());
			Set<VPoint> bPoints = new HashSet<>(b.getPoints());
			return aPoints.equals(bPoints);
		}

		@Override
		public int hash(VTriangle a) {
			return 0;
		}
	}

	@Test
	public void testEdgeVertexRelation() {
		List<VPoint> points = new ArrayList<>();
		int width = 300;
		int height = 300;
		Random r = new Random();
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

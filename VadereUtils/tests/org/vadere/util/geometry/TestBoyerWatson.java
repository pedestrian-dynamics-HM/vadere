package org.vadere.util.geometry;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.data.DAG;
import org.vadere.util.triangulation.DelaunayTriangulation;
import org.vadere.util.triangulation.DAGElement;
import org.vadere.util.geometry.data.Face;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestBoyerWatson {

	@Before
	public void setUp() throws Exception {}

	@Test
	public void testFaceIterator() {
		VPoint p1 = new VPoint(0,0);
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

		DelaunayTriangulation<VPoint> boyerWatsonImproved = new DelaunayTriangulation<>(points, (x, y) -> new VPoint(x, y));
		boyerWatsonImproved.compute();
		Collection<VTriangle> triangulation = boyerWatsonImproved.getTriangles();
		triangulation.forEach(System.out::print);
	}

	@Test
	public void testSplit() {
		VPoint p1 = new VPoint(0,0);
		VPoint p2 = new VPoint(50, 0);
		VPoint p3 = new VPoint(25, 25);
		VPoint centerPoint = new VPoint(25, 10);

		Set<VPoint> points = new HashSet<>();
		points.add(p1);
		points.add(p2);
		points.add(p3);

		Face<VPoint> face = Face.of(p1,p2,p3);
		DAG<DAGElement<VPoint>> dag = new DAG<>(new DAGElement<>(face, Triple.of(p1,p2,p3)));

		DelaunayTriangulation<VPoint> boyerWatsonImproved = new DelaunayTriangulation<>(points, (x, y) -> new VPoint(x, y));
		DAG<DAGElement<VPoint>> result = boyerWatsonImproved.split(centerPoint, dag);
		Set<VTriangle> triangulation = new HashSet<>(result.collectLeafs().stream().map(dagElement -> dagElement.getTriangle()).collect(Collectors.toList()));
		Set<VTriangle> expectedResult = new HashSet<>(Arrays.asList(new VTriangle(p1, p2, centerPoint), new VTriangle(p2, p3, centerPoint), new VTriangle(p1, p3, centerPoint)));
		assertTrue(testTriangulationEquality(triangulation, expectedResult));
	}

	@Test
	public void testPerformance() {
		Set<VPoint> points = new HashSet<>();
		int width = 300;
		int height = 300;
		Random r = new Random();

		int numberOfPoints = 100000;

		for(int i=0; i< numberOfPoints; i++) {
			VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
			points.add(point);
		}

		long ms = System.currentTimeMillis();
		DelaunayTriangulation<VPoint> bw = new DelaunayTriangulation<>(points, (x, y) -> new VPoint(x, y));
		bw.compute();
		System.out.println("runtime of the BowyerWatson for " + numberOfPoints + " vertices =" + (System.currentTimeMillis() - ms));
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

	private static boolean testTriangleEquality(final VTriangle triangle1, final VTriangle triangle2) {
		return	(triangle2.p1.equals(triangle1.p1) && triangle2.p2.equals(triangle1.p2) && triangle2.p3.equals(triangle1.p3)) ||
				(triangle2.p1.equals(triangle1.p1) && triangle2.p2.equals(triangle1.p3) && triangle2.p3.equals(triangle1.p2)) ||
				(triangle2.p1.equals(triangle1.p2) && triangle2.p2.equals(triangle1.p1) && triangle2.p3.equals(triangle1.p3)) ||
				(triangle2.p1.equals(triangle1.p2) && triangle2.p2.equals(triangle1.p3) && triangle2.p3.equals(triangle1.p1)) ||
				(triangle2.p1.equals(triangle1.p3) && triangle2.p2.equals(triangle1.p2) && triangle2.p3.equals(triangle1.p1)) ||
				(triangle2.p1.equals(triangle1.p3) && triangle2.p2.equals(triangle1.p1) && triangle2.p3.equals(triangle1.p2));
	}
}

package org.vadere.geometry;

import org.apache.commons.math3.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.vadere.meshing.WeilerAtherton;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestWeilerAtherton {

	@Before
	public void setUp() throws Exception {}

	@Test
	public void testSubtraction() {
		VRectangle subject = new VRectangle(0, 0, 5, 5);
		VRectangle clipper = new VRectangle(-1, -1, 7, 5);
		VPolygon expectedResult = new VPolygon(new VRectangle(0, 4, 5, 1));
		List<VPolygon> originalList = Arrays.asList(new VPolygon(subject), new VPolygon(clipper));
		WeilerAtherton weilerAtherton = new WeilerAtherton(originalList);
		Optional<VPolygon> optPolygon = weilerAtherton.subtraction();
		assertTrue(optPolygon.isPresent());
		assertTrue(GeometryUtils.equalsPolygons(expectedResult, optPolygon.get()));
	}

	@Test
	public void testRectangleIntersectionSpecialCase() {
		VRectangle rec1 = new VRectangle(0, 0, 5, 5);
		VRectangle rec2 = new VRectangle(2, 0, 5, 5);
		VPolygon expectedResult = new VPolygon(new VRectangle(2,0,3,5));
		List<VPolygon> originalList = Arrays.asList(new VPolygon(rec1), new VPolygon(rec2));
		WeilerAtherton weilerAtherton = new WeilerAtherton(originalList);

		Optional<VPolygon> optPolygon = weilerAtherton.cap();
		assertTrue(optPolygon.isPresent());
		assertTrue(GeometryUtils.equalsPolygons(expectedResult, optPolygon.get()));
	}

	@Test
	public void testRectangleIntersection() {
		VRectangle rec1 = new VRectangle(0, 0, 5, 5);
		VRectangle rec2 = new VRectangle(4, 4, 2, 2);
		VRectangle expectedResult = new VRectangle(4,4,1,1);
		List<VPolygon> originalList = Arrays.asList(new VPolygon(rec1), new VPolygon(rec2));
		WeilerAtherton weilerAtherton = new WeilerAtherton(originalList);

		Optional<VPolygon> optPolygon = weilerAtherton.cap();
		assertTrue(optPolygon.isPresent());
		assertTrue(GeometryUtils.equalsPolygons(new VPolygon(expectedResult), optPolygon.get()));
	}

	@Test
	public void testNoIntersection() {
		VPolygon poly1 = GeometryUtils.toPolygon(new VPoint(0, 0), new VPoint(1, 1), new VPoint(1, -1));
		VPolygon poly2 = GeometryUtils.toPolygon(new VPoint(-0.01, 0), new VPoint(-1, 1), new VPoint(-1, -1));

		List<VPolygon> originalList = new ArrayList<>(2);
		originalList.add(poly1);
		originalList.add(poly2);

		WeilerAtherton weilerAtherton = new WeilerAtherton(originalList);
		List<VPolygon> polygonList = weilerAtherton.cup();

		assertTrue(polygonList.contains(poly1));
		assertTrue(polygonList.contains(poly2));
		assertTrue(!weilerAtherton.cap().isPresent());
		assertEquals(2, polygonList.size());
	}

	@Test
	public void testIntersection() {
		VPolygon poly1 = GeometryUtils.toPolygon(new VPoint(0, 0), new VPoint(1, 1), new VPoint(1, -1));
		VPolygon poly2 = GeometryUtils.toPolygon(new VPoint(0.3, 0), new VPoint(-1, 1), new VPoint(-1, -1));

		List<VPolygon> originalList = new ArrayList<>(2);
		originalList.add(poly1);
		originalList.add(poly2);

		WeilerAtherton weilerAtherton = new WeilerAtherton(originalList);
		List<VPolygon> polygonList = weilerAtherton.cup();
		Optional<VPolygon> capResult = weilerAtherton.cap();

		assertEquals(1, polygonList.size());
		assertTrue(capResult.isPresent());
	}

	@Test
	public void testIntersectionFaceConstructionNoIntersections() {
		VPolygon poly1 = GeometryUtils.toPolygon(new VPoint(0, 0), new VPoint(1, 1), new VPoint(1, -1));
		VPolygon poly2 = GeometryUtils.toPolygon(new VPoint(-0.01, 0), new VPoint(-1, 1), new VPoint(-1, -1));

		List<VPolygon> originalList = new ArrayList<>(2);
		originalList.add(poly1);
		originalList.add(poly2);

		WeilerAtherton weilerAtherton = new WeilerAtherton(originalList);
		Pair<PFace, PFace> pair = weilerAtherton.constructIntersectionFaces(
				poly1, new PMesh(),
				poly2, new PMesh()
		);

		PFace face1 = pair.getFirst();
		PFace face2 = pair.getSecond();

		// we need a mesh to iterate
		PMesh mesh = new PMesh();

		Set<VPoint> expectedPoints1 = new HashSet<>();
		Set<VPoint> expectedPoints2 = new HashSet<>();

		expectedPoints1.addAll(poly1.getPath());
		expectedPoints2.addAll(poly2.getPath());

		assertEquals(expectedPoints1, mesh.streamPoints(face1).map(p -> new VPoint(p)).collect(Collectors.toSet()));
		assertEquals(expectedPoints2, mesh.streamPoints(face2).map(p -> new VPoint(p)).collect(Collectors.toSet()));

	}

	@Test
	public void testIntersectionFaceConstructionIntersections() {
		VPolygon poly1 = GeometryUtils.toPolygon(new VPoint(0, 0), new VPoint(1, 1), new VPoint(1, -1));
		VPolygon poly2 = GeometryUtils.toPolygon(new VPoint(0.3, 0), new VPoint(-1, 1), new VPoint(-1, -1));

		List<VPolygon> originalList = new ArrayList<>(2);
		originalList.add(poly1);
		originalList.add(poly2);

		WeilerAtherton weilerAtherton = new WeilerAtherton(originalList);
		Pair<PFace, PFace> pair = weilerAtherton.constructIntersectionFaces(
				poly1, new PMesh(),
				poly2, new PMesh()
		);

		PFace face1 = pair.getFirst();
		PFace face2 = pair.getSecond();

		// we need a mesh to iterate
		PMesh mesh = new PMesh();

		assertEquals(5, mesh.streamPoints(face1).map(p -> new VPoint(p)).collect(Collectors.toSet()).size());
		assertEquals(5, mesh.streamPoints(face2).map(p -> new VPoint(p)).collect(Collectors.toSet()).size());
	}



}

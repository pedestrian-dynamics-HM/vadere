package org.vadere.util.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.delaunay.Face;
import org.vadere.util.delaunay.HalfEdge;
import org.vadere.util.delaunay.PointLocation;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * Created by bzoennchen on 15.11.16.
 */
public class TestSimplePointLocation {

	private static Face face1;
	private static Face face2;
	private static double EPSILON = 1.0e-10;

	@Before
	public void setUp() throws Exception {
		face1 = new Face();
		face2 = new Face();
		HalfEdge halfEdge1 = new HalfEdge(new VPoint(0,0), face1);
		HalfEdge halfEdge2 = new HalfEdge(new VPoint(3,0), face1);
		HalfEdge halfEdge3 = new HalfEdge(new VPoint(1.5,3.0), face1);

		HalfEdge halfEdge4 = new HalfEdge(new VPoint(3.0,0), face2);
		halfEdge4.setTwin(halfEdge3);
		HalfEdge halfEdge5 = new HalfEdge(new VPoint(4.5,3.0), face2);
		HalfEdge halfEdge6 = new HalfEdge(new VPoint(1.5,3.0), face2);

		halfEdge4.setNext(halfEdge5);
		halfEdge5.setNext(halfEdge6);
		halfEdge6.setNext(halfEdge4);
		face2.setEdge(halfEdge4);

		halfEdge1.setNext(halfEdge2);
		halfEdge2.setNext(halfEdge3);
		halfEdge3.setNext(halfEdge1);
		face1.setEdge(halfEdge1);
	}

	@Test
	public void testFaceIterator() {
		PointLocation pointLocation = new PointLocation(Arrays.asList(face1, face2));

		assertEquals(face1, pointLocation.getFace(new VPoint(0,0)).get());

		assertEquals(face1, pointLocation.getFace(new VPoint(1.4,1.5)).get());

		assertEquals(face1, pointLocation.getFace(new VPoint(1.4,1.5)).get());

		assertEquals(Optional.empty(), pointLocation.getFace(new VPoint(1.4,3.5)));

		assertEquals(Optional.empty(), pointLocation.getFace(new VPoint(-1.5,1.4)));

		assertEquals(face2, pointLocation.getFace(new VPoint(3.5,1.4)).get());

		assertEquals(Optional.empty(), pointLocation.getFace(new VPoint(3.5,0.2)));

		assertEquals(face2, pointLocation.getFace(new VPoint(3.0,1.5)).get());

		// edges
		assertEquals(face2, pointLocation.getFace(new VPoint(3.0, EPSILON)).get());
		assertEquals(face1, pointLocation.getFace(new VPoint(1.5,3.0 - EPSILON)).get());
		assertEquals(Optional.empty(), pointLocation.getFace(new VPoint(1.5 - EPSILON,3.0 + EPSILON)));
	}
}

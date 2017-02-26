package org.vadere.util.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.mesh.Face;
import org.vadere.util.geometry.mesh.PHalfEdge;
import org.vadere.util.triangulation.PointLocation;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Arrays;
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
		PHalfEdge halfEdge1 = new PHalfEdge(new VPoint(0,0), face1);
		PHalfEdge halfEdge2 = new PHalfEdge(new VPoint(3,0), face1);
		PHalfEdge halfEdge3 = new PHalfEdge(new VPoint(1.5,3.0), face1);

		PHalfEdge halfEdge4 = new PHalfEdge(new VPoint(3.0,0), face2);
		halfEdge4.setTwin(halfEdge3);
		PHalfEdge halfEdge5 = new PHalfEdge(new VPoint(4.5,3.0), face2);
		PHalfEdge halfEdge6 = new PHalfEdge(new VPoint(1.5,3.0), face2);

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
		PointLocation<VPoint> pointLocation = new PointLocation<>(Arrays.asList(face1, face2), (x, y) -> new VPoint(x,y));

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

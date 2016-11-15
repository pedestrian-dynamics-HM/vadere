package org.vadere.util.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.delaunay.Face;
import org.vadere.util.delaunay.HalfEdge;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;


/**
 * Created by bzoennchen on 13.11.16.
 */
public class TestFace {

	private static Face face;

	@Before
	public void setUp() throws Exception {
		face = new Face();
		HalfEdge halfEdge1 = new HalfEdge(new VPoint(0,0), face);
		HalfEdge halfEdge2 = new HalfEdge(new VPoint(3,0), face);
		HalfEdge halfEdge3 = new HalfEdge(new VPoint(1.5,3.0), face);

		halfEdge1.setNext(halfEdge2);
		halfEdge2.setNext(halfEdge3);
		halfEdge3.setNext(halfEdge1);

		face.setEdge(halfEdge1);
	}

	@Test
	public void testFaceIterator() {
		assertEquals(Arrays.asList(new VPoint(0,0), new VPoint(3,0), new VPoint(3,5)), face.getPoints());
	}
}

package org.vadere.util.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.data.Face;
import org.vadere.util.geometry.data.HalfEdge;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;


public class TestFace {

	private static Face face1;
	private static Face face2;
	private static Face boder = Face.getBorder(VPoint.class);
	private VPoint x, y, z, w;
	private HalfEdge zx ;

	@Before
	public void setUp() throws Exception {
		// first triangle xyz
		face1 = new Face();
		x = new VPoint(0,0);
		y = new VPoint(3,0);
		z = new VPoint(1.5,3.0);

		zx = new HalfEdge(x, face1);
		HalfEdge xy = new HalfEdge(y, face1);
		HalfEdge yz = new HalfEdge(z, face1);

		zx.setNext(xy);
		xy.setNext(yz);
		yz.setNext(zx);

		face1.setEdge(xy);


		// second triangle yxw
		face2 = new Face();
		w = new VPoint(1.5,-1.5);
		HalfEdge yx = new HalfEdge(x, face2);
		HalfEdge xw = new HalfEdge(w, face2);
		HalfEdge wy = new HalfEdge(y, face2);

		yx.setNext(xw);
		xw.setNext(wy);
		wy.setNext(yx);

		face2.setEdge(yx);


		xy.setTwin(yx);

		// border twins
		HalfEdge zy = new HalfEdge(y, boder);
		HalfEdge xz = new HalfEdge(z, boder);

		yz.setTwin(zy);
		zx.setTwin(xz);

		HalfEdge wx = new HalfEdge(x, boder);
		HalfEdge yw = new HalfEdge(w, boder);

		xw.setTwin(wx);
		wy.setTwin(yw);

		zy.setNext(yw);
		yw.setNext(wx);
		wx.setNext(xz);
		xz.setNext(zy);

	}

	@Test
	public void testFaceIterator() {
		assertEquals(Arrays.asList(y, z, x), face1.getPoints());
	}

	@Test
	public void testEdgeIterator() {
		Iterator<HalfEdge<VPoint>> iterator = zx.incidentPointIterator();

		Set<VPoint> neighbours = new HashSet<>();
		while (iterator.hasNext()) {
			HalfEdge<VPoint> he = iterator.next();
			neighbours.add(he.getEnd());
		}

		Set<VPoint> expectedNeighbours = new HashSet<>();
		expectedNeighbours.add(z);
		expectedNeighbours.add(y);
		expectedNeighbours.add(w);

		assertEquals(expectedNeighbours, neighbours);
	}
}

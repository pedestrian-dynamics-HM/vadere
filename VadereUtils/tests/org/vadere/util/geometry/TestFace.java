package org.vadere.util.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.data.Face;
import org.vadere.util.geometry.data.HalfEdge;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


public class TestFace {

	/**
	 * Building a geometry containing 2 triangles
	 * xyz and wyx
	 */

	private static Face face1;
	private static Face face2;
	private static Face border = Face.getBorder(VPoint.class);
	private VPoint x, y, z, w;
	private HalfEdge<VPoint> zx ;
	private HalfEdge<VPoint> xy;
	private HalfEdge<VPoint> yz;

	private HalfEdge<VPoint> wx;
	private HalfEdge<VPoint> xz;
	private HalfEdge<VPoint> yw;
	private HalfEdge<VPoint> zy;

	@Before
	public void setUp() throws Exception {
		// first triangle xyz
		face1 = new Face();
		x = new VPoint(0,0);
		y = new VPoint(3,0);
		z = new VPoint(1.5,3.0);

		zx = new HalfEdge(x, face1);
		xy = new HalfEdge(y, face1);
		yz = new HalfEdge(z, face1);

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
		zy = new HalfEdge(y, border);
		xz = new HalfEdge(z, border);

		yz.setTwin(zy);
		zx.setTwin(xz);

		wx = new HalfEdge(x, border);
		yw = new HalfEdge(w, border);
		border.setEdge(wx);

		xw.setTwin(wx);
		wy.setTwin(yw);

		zy.setNext(yw);
		yw.setNext(wx);
		wx.setNext(xz);
		xz.setNext(zy);

	}

	@Test
	public void testFaceIterator() {
		List<Face<VPoint>> incidentFaces = xy.getIncidentFaces();
		assertEquals(incidentFaces.size(), 3);
	}


	@Test
	public void testPointIterator() {
		assertEquals(Arrays.asList(y, z, x), face1.getPoints());
	}

	@Test
	public void testEdgeIterator() {
		Iterator<HalfEdge<VPoint>> iterator = zx.incidentVertexIterator();

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

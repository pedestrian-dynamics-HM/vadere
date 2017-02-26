package org.vadere.util.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.mesh.Face;
import org.vadere.util.geometry.mesh.PHalfEdge;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
	private PHalfEdge<VPoint> zx ;
	private PHalfEdge<VPoint> xy;
	private PHalfEdge<VPoint> yz;

	private PHalfEdge<VPoint> wx;
	private PHalfEdge<VPoint> xz;
	private PHalfEdge<VPoint> yw;
	private PHalfEdge<VPoint> zy;

	@Before
	public void setUp() throws Exception {
		// first triangle xyz
		face1 = new Face();
		x = new VPoint(0,0);
		y = new VPoint(3,0);
		z = new VPoint(1.5,3.0);

		zx = new PHalfEdge(x, face1);
		xy = new PHalfEdge(y, face1);
		yz = new PHalfEdge(z, face1);

		zx.setNext(xy);
		xy.setNext(yz);
		yz.setNext(zx);

		face1.setEdge(xy);


		// second triangle yxw
		face2 = new Face();
		w = new VPoint(1.5,-1.5);
		PHalfEdge yx = new PHalfEdge(x, face2);
		PHalfEdge xw = new PHalfEdge(w, face2);
		PHalfEdge wy = new PHalfEdge(y, face2);

		yx.setNext(xw);
		xw.setNext(wy);
		wy.setNext(yx);

		face2.setEdge(yx);


		xy.setTwin(yx);

		// border twins
		zy = new PHalfEdge(y, border);
		xz = new PHalfEdge(z, border);

		yz.setTwin(zy);
		zx.setTwin(xz);

		wx = new PHalfEdge(x, border);
		yw = new PHalfEdge(w, border);
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
		Iterator<PHalfEdge<VPoint>> iterator = zx.incidentVertexIterator();

		Set<VPoint> neighbours = new HashSet<>();
		while (iterator.hasNext()) {
			PHalfEdge<VPoint> he = iterator.next();
			neighbours.add(he.getEnd());
		}

		Set<VPoint> expectedNeighbours = new HashSet<>();
		expectedNeighbours.add(z);
		expectedNeighbours.add(y);
		expectedNeighbours.add(w);

		assertEquals(expectedNeighbours, neighbours);
	}
}

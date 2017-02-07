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

	@Test
	public void testDeleteEdge() {
		// remove the edge between the two triangles (face1 and face2) which should result in a single rectangle.
		HashSet<VPoint> pointSet = new HashSet<>();
		pointSet.add(x);
		pointSet.add(y);
		pointSet.add(z);
		pointSet.add(w);

		Face twinFace = xy.getTwin().getFace();
		boolean vertexDeletion = xy.deleteEdge();
		// we get a rectangle
		assertEquals(vertexDeletion, false);
		assertEquals(pointSet, new HashSet<>(twinFace.getPoints()));

		pointSet.remove(twinFace.getEdge().getEnd());
		vertexDeletion = twinFace.getEdge().deleteEdge();
		assertEquals(vertexDeletion, true);
		assertEquals(pointSet, new HashSet<>(twinFace.getPoints()));

		vertexDeletion = twinFace.getEdge().deleteEdge();
		assertEquals(vertexDeletion, true);
	}

	@Test
	public void testDeleteVertex() {
		// remove the edge between the two triangles (face1 and face2) which should result in a single rectangle.
		HashSet<VPoint> pointSet = new HashSet<>();
		Face face3 = new Face();
		VPoint u = new VPoint(9,9);
		HalfEdge<VPoint> uz = new HalfEdge<>(z, face3);
		HalfEdge<VPoint> zu = new HalfEdge<>(u, border);

		zu.setTwin(uz);

		HalfEdge<VPoint> yu = new HalfEdge<>(u, face3);
		HalfEdge<VPoint> uy = new HalfEdge<>(y, border);

		uy.setTwin(yu);

		HalfEdge<VPoint> zy = new HalfEdge<>(y, face3);
		yz.setTwin(zy);

		zy.setNext(yu);
		yu.setNext(uz);
		uz.setNext(zy);

		face3.setEdge(zy);

		zu.setNext(uy);
		uy.setNext(yw);
		xz.setNext(zu);


		pointSet.add(y);
		pointSet.add(z);
		pointSet.add(w);


		assertEquals(pointSet, wx.getIncidentPoints().stream().map(he -> he.getEnd()).collect(Collectors.toSet()));

		pointSet.add(u);
		pointSet.add(x);
		pointSet.remove(y);

		assertEquals(pointSet, zy.getIncidentPoints().stream().map(he -> he.getEnd()).collect(Collectors.toSet()));

		yu.deleteVertex();

		pointSet.remove(u);
		assertEquals(pointSet, zy.getIncidentPoints().stream().map(he -> he.getEnd()).collect(Collectors.toSet()));
	}

}

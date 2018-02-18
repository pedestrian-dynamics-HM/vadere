package org.vadere.util.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PMesh;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.shapes.VPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TestPFace {

	/**
	 * Building a geometry containing 2 triangles
	 * xyz and wyx
	 */
	private IMesh<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> mesh;
	private PFace face1;
	private PFace face2;
	private PFace border;
	private PVertex<VPoint> x, y, z, w;
	private PHalfEdge<VPoint> zx ;
	private PHalfEdge<VPoint> xy;
	private PHalfEdge<VPoint> yz;

	private PHalfEdge<VPoint> wx;
	private PHalfEdge<VPoint> xz;
	private PHalfEdge<VPoint> yw;
	private PHalfEdge<VPoint> zy;

	@Before
	public void setUp() throws Exception {
		mesh = new PMesh<>((x, y) -> new VPoint(x, y));
		border = mesh.createFace(true);

		// first triangle xyz
		face1 = mesh.createFace();
		x = mesh.insertVertex(0, 0);
		y = mesh.insertVertex(3, 0);
		z = mesh.insertVertex(1.5,3.0);

		zx = mesh.createEdge(x, face1);
		mesh.setEdge(x, zx);
		xy = mesh.createEdge(y, face1);
		mesh.setEdge(y, xy);
		yz = mesh.createEdge(z, face1);
		mesh.setEdge(z, yz);

		mesh.setNext(zx, xy);
		mesh.setNext(xy, yz);
		mesh.setNext(yz, zx);

		mesh.setEdge(face1, xy);


		// second triangle yxw
		face2 = mesh.createFace();
		w = mesh.insertVertex(1.5,-1.5);

		PHalfEdge yx = mesh.createEdge(x, face2);
		PHalfEdge xw = mesh.createEdge(w, face2);
		PHalfEdge wy = mesh.createEdge(y, face2);

		mesh.setNext(yx, xw);
		mesh.setNext(xw, wy);
		mesh.setNext(wy, yx);

		mesh.setEdge(face2, yx);

		mesh.setTwin(xy, yx);

		// border twins
		zy = mesh.createEdge(y, border);
		xz = mesh.createEdge(z, border);

		mesh.setTwin(yz, zy);
		mesh.setTwin(zx, xz);

		wx = mesh.createEdge(x, border);
		yw = mesh.createEdge(w, border);
		mesh.setEdge(w, yw);

		mesh.setEdge(border, wx);
		mesh.setTwin(xw, wx);
		mesh.setTwin(wy, yw);


		mesh.setNext(zy, yw);
		mesh.setNext(yw, wx);
		mesh.setNext(wx, xz);
		mesh.setNext(xz, zy);
	}


	@Test
	public void testFaceIterator() {
		mesh.getAdjacentFacesIt(xy);
		List<PFace<VPoint>> incidentFaces = mesh.getAdjacentFaces(xy);;
		assertEquals(incidentFaces.size(), 3);
	}

	@Test
	public void testPointIterator() {
		assertEquals(new ArrayList(Arrays.asList(y, z, x)), mesh.getVertices(face1));
	}

	@Test
	public void testEdgeOfVertex() {
		mesh.streamEdges().forEach(edge -> assertEquals(mesh.getVertex(edge), mesh.getVertex(mesh.getEdge(mesh.getVertex(edge)))));
	}

	@Test
	public void testEdgeIterator() {
		List<PVertex<VPoint>> adjacentVertices = mesh.getAdjacentVertices(zx);
		Set<PVertex<VPoint>> neighbours = new HashSet<>(adjacentVertices);
		Set<PVertex<VPoint>> expectedNeighbours = new HashSet<>();
		expectedNeighbours.add(z);
		expectedNeighbours.add(y);
		expectedNeighbours.add(w);
		assertEquals(expectedNeighbours, neighbours);
	}
}

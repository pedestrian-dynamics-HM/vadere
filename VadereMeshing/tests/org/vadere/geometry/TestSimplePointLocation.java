package org.vadere.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.gen.PointLocation;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * Created by bzoennchen on 15.11.16.
 */
public class TestSimplePointLocation {

	private static PFace face1;
	private static PFace face2;
	private static PFace border;
	private static double EPSILON = 1.0e-10;
	private IMesh<PVertex, PHalfEdge, PFace> mesh;

	@Before
	public void setUp() throws Exception {
		mesh = new PMesh();
		face1 = mesh.createFace();
		face2 = mesh.createFace();
		border = mesh.createFace(true);


		PVertex x = mesh.createVertex(0,0);
		PVertex y = mesh.createVertex(3,0);
		PVertex z = mesh.createVertex(1.5,3.0);
		PVertex w = mesh.createVertex(4.5,3.0);


		PHalfEdge zx = mesh.createEdge(x, face1);
		PHalfEdge xz = mesh.createEdge(z, border);
		mesh.setTwin(zx, xz);
		mesh.setEdge(x, zx);


		PHalfEdge xy = mesh.createEdge(y, face1);
		PHalfEdge yx = mesh.createEdge(x, border);
		mesh.setTwin(xy, yx);
		mesh.setEdge(y, xy);


		PHalfEdge yz = mesh.createEdge(z, face1);
		PHalfEdge zy = mesh.createEdge(y, face2);
		mesh.setTwin(zy, yz);
		mesh.setEdge(z, yz);


		PHalfEdge yw = mesh.createEdge(w, face2);
		PHalfEdge wy = mesh.createEdge(y, border);
		mesh.setTwin(yw, wy);
		mesh.setEdge(w, yw);

		PHalfEdge wz = mesh.createEdge(z, face2);
		PHalfEdge zw = mesh.createEdge(w, face2);
		mesh.setTwin(wz, zw);

		mesh.setNext(zy, yw);
		mesh.setNext(yw, wz);
		mesh.setNext(wz, zy);

		mesh.setEdge(face2, zy);

		mesh.setNext(zx, xy);
		mesh.setNext(xy, yz);
		mesh.setNext(yz, zx);


		mesh.setNext(xz, zw);
		mesh.setNext(zw, wy);
		mesh.setNext(wy, yx);
		mesh.setNext(yx, xz);

		mesh.setEdge(face1, zx);

		mesh.setEdge(border, xz);
	}

	@Test
	public void testFaceIterator() {
		PointLocation pointLocation = new PointLocation(Arrays.asList(face1, face2), mesh);

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

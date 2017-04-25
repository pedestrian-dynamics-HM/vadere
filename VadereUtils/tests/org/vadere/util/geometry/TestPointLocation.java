package org.vadere.util.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.mesh.impl.PFace;
import org.vadere.util.geometry.mesh.impl.PHalfEdge;
import org.vadere.util.geometry.mesh.impl.PMesh;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.ITriConnectivity;
import org.vadere.util.triangulation.PointLocation;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * This test class contains tests for the point location problem for a mesh i.e.
 * given a point (x,y) find the face which contains (x,y) or find the nearest vertex
 * of the mesh.
 */
public class TestPointLocation {

	private static PFace face1;
	private static PFace face2;
	private static PFace border;
	private static double EPSILON = 1.0e-10;
	private IMesh<VPoint, PHalfEdge<VPoint>, PFace<VPoint>> mesh;
	private ITriConnectivity<VPoint, PHalfEdge<VPoint>, PFace<VPoint>> triConnectivity;

	/**
	 * Sets up a mesh consisting of 2 triangles and 1 border face.
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		mesh = new PMesh<>((x, y) -> new VPoint(x, y));
		face1 = mesh.createFace();
		face2 = mesh.createFace();
		border = mesh.createFace(true);

		VPoint x = mesh.insertVertex(0, 0);
		VPoint y = mesh.insertVertex(1.5,3.0);
		VPoint z = mesh.insertVertex(3.0,0);

		VPoint w = mesh.insertVertex(4.5,3.0);

		PHalfEdge xy = mesh.createEdge(y, face1);
		PHalfEdge yx = mesh.createEdge(x, border);
		mesh.setTwin(xy, yx);

		PHalfEdge yz = mesh.createEdge(z, face1);

		PHalfEdge zx = mesh.createEdge(x, face1);
		PHalfEdge xz = mesh.createEdge(z, border);
		mesh.setTwin(zx, xz);

		PHalfEdge zy = mesh.createEdge(y, face2);
		mesh.setTwin(yz, zy);
		PHalfEdge yw = mesh.createEdge(w, face2);
		PHalfEdge wy = mesh.createEdge(y, border);
		mesh.setTwin(yw, wy);

		PHalfEdge wz = mesh.createEdge(z, face2);
		PHalfEdge zw = mesh.createEdge(w, border);
		mesh.setTwin(wz, zw);

		mesh.setNext(zy, yw);
		mesh.setNext(yw, wz);
		mesh.setNext(wz, zy);

		mesh.setEdge(face2, zy);

		mesh.setNext(xy, yz);
		mesh.setNext(yz, zx);
		mesh.setNext(zx, xy);

		mesh.setEdge(face1, yz);

		mesh.setNext(yx, xz);
		mesh.setNext(xz, zw);
		mesh.setNext(zw, wy);
		mesh.setNext(wy, yx);

		mesh.setEdge(border, yx);

		triConnectivity = new ITriConnectivity() {
			@Override
			public boolean isIllegal(IHalfEdge edge) {
				return true;
			}

			@Override
			public IMesh<VPoint, PHalfEdge<VPoint>, PFace<VPoint>> getMesh() {
				return mesh;
			}

			@Override
			public Iterator<VPoint> iterator() {
				return null;
			}
		};
	}

	@Test
	public void testDirectFaceLocation() {
		assertEquals(face1, triConnectivity.locate(0, 0).get());

		assertEquals(face1, triConnectivity.locate(1.4,1.5).get());

		assertEquals(face1, triConnectivity.locate(1.4,1.5).get());

		assertEquals(border, triConnectivity.locate(1.4,3.5).get());

		assertEquals(border, triConnectivity.locate(-1.5,1.4).get());

		assertEquals(face2, triConnectivity.locate(3.5,1.4).get());

		assertEquals(border, triConnectivity.locate(3.5,0.2).get());

		assertEquals(face2, triConnectivity.locate(3.0,1.5).get());

		assertEquals(face2, triConnectivity.locate(4.5,3.0).get());

		assertEquals(face1, triConnectivity.locate(0, 0).get());

		assertEquals(face2, triConnectivity.locate(3.0, EPSILON).get());

		assertEquals(face1, triConnectivity.locate(1.5,3.0 - EPSILON).get());

		assertEquals(border, triConnectivity.locate(1.5 - EPSILON,3.0 + EPSILON).get());
	}

	@Test
	public void testDirectVertexLocation() {

	}
}

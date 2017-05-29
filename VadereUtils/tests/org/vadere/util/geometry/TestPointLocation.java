package org.vadere.util.geometry;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.mesh.impl.PFace;
import org.vadere.util.geometry.mesh.impl.PHalfEdge;
import org.vadere.util.geometry.mesh.impl.PMesh;
import org.vadere.util.geometry.mesh.impl.PVertex;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.ITriConnectivity;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;

/**
 * This test class triangleContains tests for the point location problem for a mesh i.e.
 * given a point (x,y) find the face which triangleContains (x,y) or find the nearest vertex
 * of the mesh.
 */
public class TestPointLocation {

	private static PFace face1;
	private static PFace face2;
	private static PFace border;
	private static double EPSILON = 1.0e-10;
	private IMesh<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> mesh;
	private ITriConnectivity<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> triConnectivity;

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

		PVertex<VPoint> x = mesh.insertVertex(0, 0);
		PVertex<VPoint> y = mesh.insertVertex(1.5,3.0);
		PVertex<VPoint> z = mesh.insertVertex(3.0,0);
		PVertex<VPoint> w = mesh.insertVertex(4.5,3.0);

		PHalfEdge xy = mesh.createEdge(y, face1);
		mesh.setEdge(y, xy);
		PHalfEdge yx = mesh.createEdge(x, border);
		mesh.setEdge(x, yx);
		mesh.setTwin(xy, yx);

		PHalfEdge yz = mesh.createEdge(z, face1);
		mesh.setEdge(z, yz);

		PHalfEdge zx = mesh.createEdge(x, face1);
		PHalfEdge xz = mesh.createEdge(z, border);
		mesh.setTwin(zx, xz);

		PHalfEdge zy = mesh.createEdge(y, face2);
		mesh.setTwin(yz, zy);
		PHalfEdge yw = mesh.createEdge(w, face2);
		mesh.setEdge(w, yw);
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
			public boolean isIllegal(IHalfEdge edge, IVertex p) {
				return true;
			}

			@Override
			public IHalfEdge insert(@NotNull IPoint point, @NotNull IFace face) {
				return null;
			}

			@Override
			public void legalizeNonRecursive(@NotNull IHalfEdge edge, IVertex p) {

			}

			@Override
			public IMesh<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> getMesh() {
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
		assertEquals(face1, triConnectivity.locateFace(0, 0).get());

		assertEquals(face1, triConnectivity.locateFace(1.4,1.5).get());

		assertEquals(face1, triConnectivity.locateFace(1.4,1.5).get());

		assertEquals(border, triConnectivity.locateFace(1.4,3.5).get());

		assertEquals(border, triConnectivity.locateFace(-1.5,1.4).get());

		assertEquals(face2, triConnectivity.locateFace(3.5,1.4).get());

		assertEquals(border, triConnectivity.locateFace(3.5,0.2).get());

		assertEquals(face2, triConnectivity.locateFace(3.0,1.5).get());

		assertEquals(face2, triConnectivity.locateFace(4.5,3.0).get());

		assertEquals(face1, triConnectivity.locateFace(0, 0).get());

		assertEquals(face2, triConnectivity.locateFace(3.0, EPSILON).get());

		assertEquals(face1, triConnectivity.locateFace(1.5,3.0 - EPSILON).get());

		assertEquals(border, triConnectivity.locateFace(1.5 - EPSILON,3.0 + EPSILON).get());
	}

	@Test
	public void testDirectVertexLocation() {

	}
}

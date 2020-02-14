package org.vadere.geometry;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.ITriConnectivity;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;

/**
 * This test class tests for the point location problem for a mesh i.e.
 * given a point (x,y) find the face which triangleContains (x,y) or find the nearest vertex
 * of the mesh.
 */
public class TestPointLocation {

	private static PFace face1;
	private static PFace face2;
	private static PFace border;
	private static double EPSILON = 1.0e-6;
	private IMesh<PVertex, PHalfEdge, PFace> mesh;
	private ITriConnectivity<PVertex, PHalfEdge, PFace> triConnectivity;

	/**
	 * Sets up a mesh consisting of 2 triangles and 1 border face.
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		mesh = new PMesh();
		face1 = mesh.createFace();
		face2 = mesh.createFace();
		border = mesh.getBorder();

		PVertex x = mesh.insertVertex(0, 0);
		PVertex y = mesh.insertVertex(1.5,3.0);
		PVertex z = mesh.insertVertex(3.0,0);
		PVertex w = mesh.insertVertex(4.5,3.0);

		PHalfEdge xy = mesh.createEdge(y, border);
		mesh.setEdge(y, xy);
		PHalfEdge yx = mesh.createEdge(x, face1);
		mesh.setEdge(x, yx);
		mesh.setTwin(xy, yx);

		PHalfEdge yz = mesh.createEdge(z, face2);
		mesh.setEdge(z, yz);

		PHalfEdge zx = mesh.createEdge(x, border);
		PHalfEdge xz = mesh.createEdge(z, face1);
		mesh.setTwin(zx, xz);

		PHalfEdge zy = mesh.createEdge(y, face1);
		mesh.setTwin(yz, zy);
		PHalfEdge yw = mesh.createEdge(w, border);
		mesh.setEdge(w, yw);
		PHalfEdge wy = mesh.createEdge(y, face2);
		mesh.setTwin(yw, wy);

		PHalfEdge wz = mesh.createEdge(z, border);
		PHalfEdge zw = mesh.createEdge(w, face2);
		mesh.setTwin(wz, zw);

		mesh.setNext(zy, yx);
		mesh.setNext(yx, xz);
		mesh.setNext(xz, zy);

		mesh.setEdge(face1, zy);

		mesh.setNext(yz, zw);
		mesh.setNext(zw, wy);
		mesh.setNext(wy, yz);

		mesh.setEdge(face2, yz);

		mesh.setNext(zx, xy);
		mesh.setNext(xy, yw);
		mesh.setNext(yw, wz);
		mesh.setNext(wz, zx);

		mesh.setEdge(border, zx);

		triConnectivity = new ITriConnectivity() {
			@Override
			public boolean isIllegal(IHalfEdge edge, IVertex p) {
				return true;
			}

			@Override
			public boolean isIllegal(@NotNull IHalfEdge edge, @NotNull IVertex p, double eps) {
				return false;
			}

			@Override
			public IHalfEdge insert(@NotNull IPoint point, @NotNull IFace face) {
				return null;
			}

			@Override
			public void legalizeNonRecursive(@NotNull IHalfEdge edge, IVertex p) {

			}

			@Override
			public IMesh<PVertex, PHalfEdge, PFace> getMesh() {
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

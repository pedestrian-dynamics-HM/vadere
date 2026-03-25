package org.vadere.geometry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vadere.meshing.mesh.gen.mesh.ReadOnlyTriangleConnectivity;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles.PTriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.meshConnectivity.IReadOnlyTriConnectivity;

import static  org.junit.jupiter.api.Assertions.assertEquals;

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
	private ITriangleMeshBuilder<PVertex, PHalfEdge, PFace> meshBuilder;
	private IReadOnlyTriConnectivity<PVertex, PHalfEdge, PFace> triConnectivity;

	/**
	 * Sets up a mesh consisting of 2 triangles and 1 border face.
	 *
	 * @throws Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		meshBuilder = new PTriangleMeshBuilder();
		face1 = meshBuilder.faces().createAndInsert();
		face2 = meshBuilder.faces().createAndInsert();
		border = meshBuilder.getMesh().faces().getOuterBorder();

		PVertex x = meshBuilder.vertices().createAndInsert(0, 0);
		PVertex y = meshBuilder.vertices().createAndInsert(1.5,3.0);
		PVertex z = meshBuilder.vertices().createAndInsert(3.0,0);
		PVertex w = meshBuilder.vertices().createAndInsert(4.5,3.0);

		PHalfEdge xy = meshBuilder.edges().createAndInsert(y, border);
		meshBuilder.vertices().setEdge(y, xy);
		PHalfEdge yx = meshBuilder.edges().createAndInsert(x, face1);
		meshBuilder.vertices().setEdge(x, yx);
		meshBuilder.edges().setTwin(xy, yx);

		PHalfEdge yz = meshBuilder.edges().createAndInsert(z, face2);
		meshBuilder.vertices().setEdge(z, yz);

		PHalfEdge zx = meshBuilder.edges().createAndInsert(x, border);
		PHalfEdge xz = meshBuilder.edges().createAndInsert(z, face1);
		meshBuilder.edges().setTwin(zx, xz);

		PHalfEdge zy = meshBuilder.edges().createAndInsert(y, face1);
		meshBuilder.edges().setTwin(yz, zy);
		PHalfEdge yw = meshBuilder.edges().createAndInsert(w, border);
		meshBuilder.vertices().setEdge(w, yw);
		PHalfEdge wy = meshBuilder.edges().createAndInsert(y, face2);
		meshBuilder.edges().setTwin(yw, wy);

		PHalfEdge wz = meshBuilder.edges().createAndInsert(z, border);
		PHalfEdge zw = meshBuilder.edges().createAndInsert(w, face2);
		meshBuilder.edges().setTwin(wz, zw);

		meshBuilder.edges().setNext(zy, yx);
		meshBuilder.edges().setNext(yx, xz);
		meshBuilder.edges().setNext(xz, zy);

		meshBuilder.faces().setEdge(face1, zy);

		meshBuilder.edges().setNext(yz, zw);
		meshBuilder.edges().setNext(zw, wy);
		meshBuilder.edges().setNext(wy, yz);

		meshBuilder.faces().setEdge(face2, yz);

		meshBuilder.edges().setNext(zx, xy);
		meshBuilder.edges().setNext(xy, yw);
		meshBuilder.edges().setNext(yw, wz);
		meshBuilder.edges().setNext(wz, zx);

		meshBuilder.faces().setEdge(border, zx);

		triConnectivity = new ReadOnlyTriangleConnectivity<>(meshBuilder.getMesh());
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

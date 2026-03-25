package org.vadere.geometry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.*;
import org.vadere.meshing.mesh.gen.PointLocation;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Arrays;
import java.util.Optional;

import static  org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by bzoennchen on 15.11.16.
 */
public class TestSimplePointLocation {

	private static PFace face1;
	private static PFace face2;
	private static PFace border;
	private static double EPSILON = 1.0e-10;
	private IMeshBuilder<PVertex, PHalfEdge, PFace> meshBuilder;

	@BeforeEach
	public void setUp() throws Exception {
		meshBuilder = new PMeshBuilder();
		face1 = meshBuilder.faces().createAndInsert();
		face2 = meshBuilder.faces().createAndInsert();
		border = meshBuilder.faces().createAndInsertHole();


		PVertex x = meshBuilder.vertices().createAndInsert(0,0);
		PVertex y = meshBuilder.vertices().createAndInsert(3,0);
		PVertex z = meshBuilder.vertices().createAndInsert(1.5,3.0);
		PVertex w = meshBuilder.vertices().createAndInsert(4.5,3.0);


		PHalfEdge zx = meshBuilder.edges().createAndInsert(x, face1);
		PHalfEdge xz = meshBuilder.edges().createAndInsert(z, border);
		meshBuilder.edges().setTwin(zx, xz);
		meshBuilder.vertices().setEdge(x, zx);


		PHalfEdge xy = meshBuilder.edges().createAndInsert(y, face1);
		PHalfEdge yx = meshBuilder.edges().createAndInsert(x, border);
		meshBuilder.edges().setTwin(xy, yx);
		meshBuilder.vertices().setEdge(y, xy);


		PHalfEdge yz = meshBuilder.edges().createAndInsert(z, face1);
		PHalfEdge zy = meshBuilder.edges().createAndInsert(y, face2);
		meshBuilder.edges().setTwin(zy, yz);
		meshBuilder.vertices().setEdge(z, yz);


		PHalfEdge yw = meshBuilder.edges().createAndInsert(w, face2);
		PHalfEdge wy = meshBuilder.edges().createAndInsert(y, border);
		meshBuilder.edges().setTwin(yw, wy);
		meshBuilder.vertices().setEdge(w, yw);

		PHalfEdge wz = meshBuilder.edges().createAndInsert(z, face2);
		PHalfEdge zw = meshBuilder.edges().createAndInsert(w, face2);
		meshBuilder.edges().setTwin(wz, zw);

		meshBuilder.edges().setNext(zy, yw);
		meshBuilder.edges().setNext(yw, wz);
		meshBuilder.edges().setNext(wz, zy);

		meshBuilder.faces().setEdge(face2, zy);

		meshBuilder.edges().setNext(zx, xy);
		meshBuilder.edges().setNext(xy, yz);
		meshBuilder.edges().setNext(yz, zx);


		meshBuilder.edges().setNext(xz, zw);
		meshBuilder.edges().setNext(zw, wy);
		meshBuilder.edges().setNext(wy, yx);
		meshBuilder.edges().setNext(yx, xz);

		meshBuilder.faces().setEdge(face1, zx);

		meshBuilder.faces().setEdge(border, xz);
	}

	@Test
	public void testFaceIterator() {
		PointLocation pointLocation = new PointLocation(Arrays.asList(face1, face2), meshBuilder.getMesh());

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

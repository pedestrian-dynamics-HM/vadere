package org.vadere.geometry.triangulation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PMesh;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;

import static  org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestSimplePointLocation {

	private IMesh<PVertex, PHalfEdge, PFace> mesh;
	private IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation;
	private long numberOfPoints = 100;

	@BeforeEach
	public void setUp() throws Exception {
		mesh = new PMesh();
		triangulation = IIncrementalTriangulation.generateRandomTriangulation(numberOfPoints);
	}

	@Test
	public void testLocateAllVertices() {
		assertTrue(triangulation.getMesh().vertices().getAll().size() > numberOfPoints * 0.1);
		triangulation.getMesh().vertices().getAll().forEach(p ->
				assertTrue(triangulation.getMesh().readConnectivity().locate(p.getX(), p.getY()).isPresent()));
	}



}

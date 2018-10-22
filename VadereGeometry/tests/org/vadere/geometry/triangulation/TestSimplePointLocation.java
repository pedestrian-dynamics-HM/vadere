package org.vadere.geometry.triangulation;

import org.junit.Before;
import org.junit.Test;
import org.vadere.geometry.mesh.gen.PFace;
import org.vadere.geometry.mesh.gen.PHalfEdge;
import org.vadere.geometry.mesh.gen.PMesh;
import org.vadere.geometry.mesh.gen.PVertex;
import org.vadere.geometry.mesh.inter.IMesh;
import org.vadere.geometry.mesh.inter.ITriangulation;
import org.vadere.geometry.shapes.VPoint;

import static org.junit.Assert.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestSimplePointLocation {

	private IMesh<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> mesh;
	private ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> triangulation;
	private long numberOfPoints = 100;

	@Before
	public void setUp() throws Exception {
		mesh = new PMesh<>((x, y) -> new VPoint(x, y));
		triangulation = ITriangulation.generateRandomTriangulation(numberOfPoints);
	}

	@Test
	public void testLocateAllVertices() {
		assertTrue(triangulation.getMesh().getVertices().size() > numberOfPoints * 0.1);
		triangulation.getMesh().getVertices().forEach(p -> assertTrue(triangulation.locateFace(p.getX(), p.getY()).isPresent()));
	}



}

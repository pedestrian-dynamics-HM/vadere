package org.vadere.util.triangulation;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.mesh.impl.PFace;
import org.vadere.util.geometry.mesh.impl.PHalfEdge;
import org.vadere.util.geometry.mesh.impl.PMesh;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.VPoint;

import static org.junit.Assert.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestSimplePointLocation {

	private IMesh<VPoint, PHalfEdge<VPoint>, PFace<VPoint>> mesh;
	private ITriangulation<VPoint, PHalfEdge<VPoint>, PFace<VPoint>> triangulation;
	private long numberOfPoints = 100;

	@Before
	public void setUp() throws Exception {
		mesh = new PMesh<>((x, y) -> new VPoint(x, y));
		triangulation = ITriangulation.generateRandomTriangulation(numberOfPoints);
		triangulation.compute();
	}

	@Test
	public void testLocateAllVertices() {
		assertTrue(triangulation.getMesh().getVertices().size() > numberOfPoints * 0.1);
		triangulation.getMesh().getVertices().forEach(p -> assertTrue(triangulation.locate(p.getX(), p.getY()).isPresent()));
	}



}

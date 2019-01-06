package org.vadere.geometry.triangulation;

import org.junit.Before;
import org.junit.Test;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.util.geometry.shapes.VPoint;

import static org.junit.Assert.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestSimplePointLocation {

	private IMesh<VPoint, Object, Object, PVertex<VPoint, Object, Object>, PHalfEdge<VPoint, Object, Object>, PFace<VPoint, Object, Object>> mesh;
	private IIncrementalTriangulation<VPoint, Object, Object, PVertex<VPoint, Object, Object>, PHalfEdge<VPoint, Object, Object>, PFace<VPoint, Object, Object>> triangulation;
	private long numberOfPoints = 100;

	@Before
	public void setUp() throws Exception {
		mesh = new PMesh<>((x, y) -> new VPoint(x, y));
		triangulation = IIncrementalTriangulation.generateRandomTriangulation(numberOfPoints);
	}

	@Test
	public void testLocateAllVertices() {
		assertTrue(triangulation.getMesh().getVertices().size() > numberOfPoints * 0.1);
		triangulation.getMesh().getVertices().forEach(p -> assertTrue(triangulation.locateFace(p.getX(), p.getY()).isPresent()));
	}



}

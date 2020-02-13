package org.vadere.geometry.mesh;

import org.junit.Before;
import org.junit.Test;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.impl.PTriangulation;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestMeshManipulations {

	/**
	 * Building a geometry containing 2 triangles
	 * xyz and wyx
	 */
	private PTriangulation triangulation;
	private VRectangle bound;

	@Before
	public void setUp() throws Exception {
		bound = new VRectangle(-1, -1, 12, 12);
		triangulation = IIncrementalTriangulation.createVPTriangulation(bound);
		triangulation.insert(new VPoint(0,0));
		triangulation.insert(new VPoint(2,0));
		triangulation.insert(new VPoint(1, 1.5));
		triangulation.insert(new VPoint(4,2.5));
		triangulation.insert(new VPoint(2, 2.6));
		triangulation.insert(new VPoint(10, 10));
		triangulation.insert(new VPoint(10, 0));
		triangulation.insert(new VPoint(0, 10));
		triangulation.insert(new VPoint(6, 8));
		triangulation.insert(new VPoint(7, 6));
		triangulation.insert(new VPoint(7.8, 3));
		triangulation.insert(new VPoint(5.7, 4));
		triangulation.finish();
	}

	@Test
	public void testRemoveSomeFacesByHoleCreation() {
		int numberOfFaces = triangulation.getMesh().getNumberOfFaces();

		// locate a face / triangle containing (4, 5)
		PFace face = triangulation.locate(6, 6).get();

		// merge faces until infinity, therefore consumes all faces!
		Predicate<PFace> mergePredicate = f -> true;

		int maxDept = 1;

		// since max dept is equal to 1 we merge 4 (the face and its 3 neighbours) triangles into 1 polygon
		assertTrue(triangulation.mergeFaces(face, mergePredicate, true, maxDept).isPresent());

		// and therefore the number of faces decreases by 3!
		assertEquals(numberOfFaces-3, triangulation.getMesh().getNumberOfFaces());
	}

	@Test
	public void testRemoveAllFaces() {
		// locate a face / triangle containing (4, 5)
		PFace face = triangulation.locate(4, 5).get();

		// merge faces until infinity, therefore consumes all faces!
		Predicate<PFace> shrinkCondition = f -> true;

		triangulation.shrinkBorder(shrinkCondition, true);

		assertEquals(0, triangulation.getMesh().getNumberOfFaces());
		assertEquals(0, triangulation.getMesh().getNumberOfHoles());
		assertEquals(0, triangulation.getMesh().getNumberOfVertices());
		triangulation.getMesh().garbageCollection();
		assertEquals(0, triangulation.getMesh().getNumberOfEdges());
	}
}

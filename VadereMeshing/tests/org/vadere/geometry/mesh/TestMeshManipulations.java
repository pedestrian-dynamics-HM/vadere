package org.vadere.geometry.mesh;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.VPTriangulation;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.meshing.mesh.gen.MeshPanel;

import java.util.List;
import java.util.function.Predicate;

import javax.swing.*;

/**
 * @author Benedikt Zoennchen
 */
public class TestMeshManipulations {

	/**
	 * Building a geometry containing 2 triangles
	 * xyz and wyx
	 */
	private VPTriangulation triangulation;
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
	public void testRemoveFace() {

	}

	public static void main(String... args) {
		TestMeshManipulations test = new TestMeshManipulations();
		try {
			test.setUp();
		} catch (Exception e) {
			e.printStackTrace();
		}

		/*test.triangulation.removeFace(test.triangulation.locateFace(3, 5).get(), true);
		test.triangulation.removeFace(test.triangulation.locateFace(8, 3).get(), true);
		test.triangulation.removeFace(test.triangulation.locateFace(4, 5).get(), true);
		test.triangulation.removeFace(test.triangulation.locateFace(4, 9).get(), true);*/

		PFace<VPoint> face = test.triangulation.locateFace(4, 5).get();
		List<VPoint> points = test.triangulation.getMesh().getPoints(face);
		VPoint incetner = GeometryUtils.getIncenter(points.get(0), points.get(1), points.get(2));

		Predicate<PFace<VPoint>> mergePredicate = f -> {
			List<VPoint> pList = test.triangulation.getMesh().getPoints(f);
			VPoint icetner = GeometryUtils.getIncenter(pList.get(0), pList.get(1), pList.get(2));
			return icetner.distance(incetner) < 500;
		};

		//Utils.getCentroid()
		test.triangulation.createHole(test.triangulation.locateFace(4, 5).get(), mergePredicate, true);
		//PFace<VPoint> face = ;
		MeshPanel<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> panel = new MeshPanel<>(test.triangulation.getMesh(),
				f -> test.triangulation.getMesh().isHole(f), 800, 800, test.bound);
		JFrame frame = panel.display();
		frame.setVisible(true);
		panel.repaint();
	}
}

package org.vadere.util.geometry.mesh;

import com.google.common.collect.Iterables;

import org.junit.Before;
import org.junit.Test;
import org.lwjgl.system.CallbackI;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PMesh;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.impl.VPTriangulation;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestTriangulationOperations {

	private VPTriangulation triangulation;
	private IMesh<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> mesh;
	private VPoint collapsePoint = new VPoint(0.5, 0);
	private List<VPoint> points = new ArrayList<>();
	private VRectangle bound = new VRectangle(-0.5, -0.5, 2.0, 2.0);

	@Before
	public void setUp() throws Exception {
		triangulation = ITriangulation.createVPTriangulation(bound);
		points.add(new VPoint(0,0));
		points.add(collapsePoint);
		points.add(new VPoint(1, 0));
		points.add(new VPoint(0.5, 1));
		triangulation.insert(points);
		triangulation.finish();
		mesh = triangulation.getMesh();
	}

	@Test
	public void testCollapse() {
		PVertex<VPoint> vertex = mesh
				.streamVertices().filter(v -> mesh.getPoint(v).equals(collapsePoint))
				.findAny().get();

		assertTrue(new HashSet<>(points).equals(new HashSet<>(mesh.getPoints())));

		assertTrue(mesh.getFaces().size() == 2);

		triangulation.collapseAtBoundary(vertex, true);

		assertTrue(mesh.getFaces().size() == 1);

		assertFalse(new HashSet<>(points).equals(new HashSet<>(mesh.getPoints())));

		PFace<VPoint> face = triangulation.getMesh().getFaces().get(0);

		assertTrue(mesh.streamEdges(face).allMatch(e -> mesh.getFace(e).equals(face)));

		assertTrue(mesh.streamEdges(mesh.getBorder()).allMatch(e -> mesh.getFace(e).equals(mesh.getBorder())));

		assertTrue(mesh.streamVertices().allMatch(v -> mesh.isAtBorder(v)));

		assertTrue(mesh.streamVertices().allMatch(v -> mesh.getFace(v).equals(mesh.getBorder()) || mesh.getFace(v).equals(face)));

		assertTrue(mesh.streamVertices().allMatch(v -> mesh.getVertex(mesh.getEdge(v)).equals(v)));

		points.remove(collapsePoint);
		assertTrue(new HashSet<>(points).equals(new HashSet<>(mesh.getPoints())));
	}

	@Test
	public void testIsValid() {
		assertTrue(triangulation.isValid());
	}

	@Test
	public void testRecompute() {
		triangulation.recompute();
	}
}

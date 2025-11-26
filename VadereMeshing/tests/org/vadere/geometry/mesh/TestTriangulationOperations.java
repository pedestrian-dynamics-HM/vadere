package org.vadere.geometry.mesh;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.impl.PTriangulation;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.IMeshWithDataStorage;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PDelaunayTriangulator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


/**
 * @author Benedikt Zoennchen
 */
public class TestTriangulationOperations {

	private PTriangulation triangulation;
	private VPoint collapsePoint = new VPoint(0.5, 0);
	private List<IPoint> points = new ArrayList<>();
	private VRectangle bound = new VRectangle(-0.5, -0.5, 2.0, 2.0);

	@BeforeEach
	public void setUp() throws Exception {
		triangulation = IIncrementalTriangulation.createVPTriangulation(bound);
		points.add(new VPoint(0,0));
		points.add(collapsePoint);
		points.add(new VPoint(1, 0));
		points.add(new VPoint(0.5, 1));
		triangulation.insert(points);
		triangulation.finish();
	}

	@Test
	public void testCollapse() {
		IMeshWithDataStorage<PVertex, PHalfEdge, PFace> meshWithDataStorage = triangulation.getMeshBuilder().getMeshWithDataStorage();
		var mesh = meshWithDataStorage.getMesh();
		PVertex vertex = mesh
				.vertices().stream().filter(v -> mesh.vertices().toPoint(v).equals(collapsePoint))
				.findAny().get();

		assertTrue(new HashSet<>(points).equals(new HashSet<>(mesh.vertices().toPoints())));

		assertTrue(mesh.faces().getAll().size() == 2);

		triangulation.getMeshBuilder().changeConnectivity().collapse3DVertex(vertex, true);

		assertTrue(mesh.faces().getAll().size() == 1);

		assertFalse(new HashSet<>(points).equals(new HashSet<>(mesh.vertices().toPoints())));

		PFace face = triangulation.getMesh().faces().getAll().get(0);

		assertTrue(mesh.edges().streamEdgesOf(face).allMatch(e -> mesh.faces().getOf(e).equals(face)));

		assertTrue(mesh.edges().streamEdgesOf(mesh.faces().getOuterBorder()).allMatch(e -> mesh.faces().getOf(e).equals(mesh.faces().getOuterBorder())));

		assertTrue(mesh.vertices().stream().allMatch(v -> mesh.vertices().isAtBorder(v)));

		assertTrue(mesh.vertices().stream().allMatch(v -> mesh.faces().getOf(v).equals(mesh.faces().getOuterBorder()) || mesh.faces().getOf(v).equals(face)));

		assertTrue(mesh.vertices().stream().allMatch(v -> mesh.vertices().getEndOf(mesh.edges().getOf(v)).equals(v)));

		points.remove(collapsePoint);
		assertTrue(new HashSet<>(points).equals(new HashSet<>(mesh.vertices().toPoints())));
	}

	@Test
	public void testIsValid() {
		assertTrue(triangulation.getMesh().isValid());
		assertTrue(triangulation.getMesh().readConnectivity().isValid());
	}

	@Test
	public void testRecompute() {
		triangulation.recompute();
	}

	@Test
	public void testRemovePoint() {
		List<VPoint> points = Arrays.asList(new VPoint(0,0),
				new VPoint(1, 0),
				new VPoint(1, 1),
				new VPoint(0, 1),
				new VPoint(0.5, 0.5),
				new VPoint(0.3, 0.8),
				new VPoint(0.12, 0.23),
				new VPoint(0.3, 0.3),
				new VPoint(0.3, 0.6));

		var delaunayTriangulation = new PDelaunayTriangulator(points);
		var triangulation = delaunayTriangulation.generate();
		var mesh = delaunayTriangulation.getMeshBuilder().getMesh();

		assertEquals(points.size(), mesh.vertices().count());

		triangulation.remove(new VPoint(0.5, 0.5));
		assertEquals(points.size()-1, mesh.vertices().count());
		assertTrue(mesh.isValid());

		triangulation.remove(new VPoint(0.3, 0.3));
		assertEquals(points.size()-2, mesh.vertices().count());
		assertTrue(mesh.isValid());

		triangulation.remove(new VPoint(0.3, 0.6));
		assertEquals(points.size()-3, mesh.vertices().count());
		assertTrue(mesh.isValid());

		triangulation.insert(new VPoint(0.15, 0.5));
		assertEquals(points.size()-2, mesh.vertices().count());
		assertTrue(mesh.isValid());

		triangulation.remove(new VPoint(0.15, 0.5));
		assertEquals(points.size()-3, mesh.vertices().count());
		assertTrue(mesh.isValid());
	}
}

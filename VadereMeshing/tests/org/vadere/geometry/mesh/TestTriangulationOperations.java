package org.vadere.geometry.mesh;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.PVertex;
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
		IMeshWithDataStorage<PVertex, PHalfEdge, PFace> meshWithDataStorage = triangulation.getMeshWithDataStorage();
		var mesh = meshWithDataStorage.getMesh();
		PVertex vertex = mesh
				.streamVertices().filter(v -> mesh.toPoint(v).equals(collapsePoint))
				.findAny().get();

		assertTrue(new HashSet<>(points).equals(new HashSet<>(mesh.getPoints())));

		assertTrue(mesh.getFaces().size() == 2);

		triangulation.collapse3DVertex(vertex, true);

		assertTrue(mesh.getFaces().size() == 1);

		assertFalse(new HashSet<>(points).equals(new HashSet<>(mesh.getPoints())));

		PFace face = triangulation.getMesh().getFaces().get(0);

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
		assertTrue(triangulation.getMesh().isValid());
		assertTrue(triangulation.isValid());
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
		var mesh = delaunayTriangulation.getMesh();

		assertEquals(points.size(), mesh.getNumberOfVertices());

		triangulation.remove(new VPoint(0.5, 0.5));
		assertEquals(points.size()-1, mesh.getNumberOfVertices());
		assertTrue(mesh.isValid());

		triangulation.remove(new VPoint(0.3, 0.3));
		assertEquals(points.size()-2, mesh.getNumberOfVertices());
		assertTrue(mesh.isValid());

		triangulation.remove(new VPoint(0.3, 0.6));
		assertEquals(points.size()-3, mesh.getNumberOfVertices());
		assertTrue(mesh.isValid());

		triangulation.insert(new VPoint(0.15, 0.5));
		assertEquals(points.size()-2, mesh.getNumberOfVertices());
		assertTrue(mesh.isValid());

		triangulation.remove(new VPoint(0.15, 0.5));
		assertEquals(points.size()-3, mesh.getNumberOfVertices());
		assertTrue(mesh.isValid());
	}
}

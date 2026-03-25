package org.vadere.geometry.triangulation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vadere.meshing.examples.MeshExamples;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles.PTriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.ITriangleMeshWithDataStorage;
import org.vadere.meshing.utils.io.poly.MeshPolyReader;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.io.InputStream;
import java.util.Optional;
import java.util.Random;

import static  org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestPointLocation {
	private IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation;
	private Random random;
	private int numberOfWalks = 1000;
	private static Logger logger = Logger.getLogger(TestPointLocation.class);

	@BeforeEach
	public void setUp() throws Exception {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/kaiserslautern_tri.poly");
		MeshPolyReader<PVertex, PHalfEdge, PFace> meshPolyReader = new MeshPolyReader<>(PTriangleMeshBuilder::new);
		var mesh = (ITriangleMeshWithDataStorage<PVertex, PHalfEdge, PFace>) meshPolyReader.readMesh(inputStream);
		triangulation = IncrementalTriangulation.fromMeshBuilder(mesh.modify());
		random = new Random(0);
	}

	@Test
	public void testSingleLocation() {
		double x =279.2008581655762;
		double y = 480.5370815358783;

		Optional<PFace> face = triangulation.getMesh().readConnectivity().locate(x, y);
		assertTrue(face.isPresent() &&
						(triangulation.getMesh().faces().isOuterBorder(face.get()) && !triangulation.getMesh().faces().toPolygon(face.get()).contains(x,y) ||
								!triangulation.getMesh().faces().isOuterBorder(face.get()) && triangulation.getMesh().faces().toPolygon(face.get()).contains(x,y)),
				"point location failed for (" + x + "," + y + ")" + face.get());

	}

	@Test
	public void testBasicLocateAllVertices() {
		for(int i = 0; i < numberOfWalks; i++) {
			double x = triangulation.getMesh().getBound().getMinX() + random.nextDouble() * triangulation.getMesh().getBound().getWidth();
			double y = triangulation.getMesh().getBound().getMinY() + random.nextDouble() * triangulation.getMesh().getBound().getHeight();

			//System.out.println("point location for (" + x + "," + y +")" );
			Optional<PFace> face = triangulation.getMesh().readConnectivity().locate(x, y);
			assertTrue(face.isPresent() &&
							(triangulation.getMesh().faces().isOuterBorder(face.get()) && !triangulation.getMesh().faces().toPolygon(face.get()).contains(x,y) ||
									!triangulation.getMesh().faces().isOuterBorder(face.get()) && triangulation.getMesh().faces().toPolygon(face.get()).contains(x,y)),
					i + " point location failed for (" + x + "," + y + ")" + face.get());
		}
	}

	@Test
	public void testJumpAndRunLocateAllVertices() {
		for(int i = 0; i < numberOfWalks; i++) {
			double x = triangulation.getMesh().getBound().getMinX() + random.nextDouble() * triangulation.getMesh().getBound().getWidth();
			double y = triangulation.getMesh().getBound().getMinY() + random.nextDouble() * triangulation.getMesh().getBound().getHeight();

			//System.out.println("point location for (" + x + "," + y +")" );

			Optional<PFace> face = triangulation.locateFace(new VPoint(x, y));

			//System.out.println(triangulation.getMesh().toPolygon(face.get()).contains(x,y));

			assertTrue(face.isPresent() &&
							(triangulation.getMesh().faces().isOuterBorder(face.get()) && !triangulation.getMesh().faces().toPolygon(face.get()).contains(x,y) ||
									!triangulation.getMesh().faces().isOuterBorder(face.get()) && triangulation.getMesh().faces().toPolygon(face.get()).contains(x,y)),
					i + " point location failed for (" + x + "," + y + ")" + face.get());
		}

	}
}

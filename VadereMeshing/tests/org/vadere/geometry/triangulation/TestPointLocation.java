package org.vadere.geometry.triangulation;

import org.junit.Before;
import org.junit.Test;
import org.vadere.meshing.examples.MeshExamples;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.utils.io.poly.MeshPolyReader;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.io.InputStream;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestPointLocation {
	private IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation;
	private Random random;
	private int numberOfWalks = 1000;
	private static Logger logger = Logger.getLogger(TestPointLocation.class);

	@Before
	public void setUp() throws Exception {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/kaiserslautern_tri.poly");
		MeshPolyReader<PVertex, PHalfEdge, PFace> meshPolyReader = new MeshPolyReader<>(() -> new PMesh());
		var mesh = meshPolyReader.readMesh(inputStream);
		triangulation = new IncrementalTriangulation<>(mesh);
		random = new Random(0);
	}

	@Test
	public void testSingleLocation() {
		double x =279.2008581655762;
		double y = 480.5370815358783;

		Optional<PFace> face = triangulation.locate(x, y);
		assertTrue( "point location failed for (" + x + "," + y + ")" + face.get(),
				face.isPresent() &&
						(triangulation.getMesh().isBorder(face.get()) && !triangulation.getMesh().toPolygon(face.get()).contains(x,y) ||
								!triangulation.getMesh().isBorder(face.get()) && triangulation.getMesh().toPolygon(face.get()).contains(x,y)));

	}

	@Test
	public void testBasicLocateAllVertices() {
		for(int i = 0; i < numberOfWalks; i++) {
			double x = triangulation.getMesh().getBound().getMinX() + random.nextDouble() * triangulation.getMesh().getBound().getWidth();
			double y = triangulation.getMesh().getBound().getMinY() + random.nextDouble() * triangulation.getMesh().getBound().getHeight();

			//System.out.println("point location for (" + x + "," + y +")" );
			Optional<PFace> face = triangulation.locate(x, y);
			assertTrue( i + " point location failed for (" + x + "," + y + ")" + face.get(),
					face.isPresent() &&
							(triangulation.getMesh().isBorder(face.get()) && !triangulation.getMesh().toPolygon(face.get()).contains(x,y) ||
									!triangulation.getMesh().isBorder(face.get()) && triangulation.getMesh().toPolygon(face.get()).contains(x,y)));
		}

		/*double x = 93.34105388798974;
		double y = 113.51983761577478;

		double x1 = 201.51868233333335;
		double y1 = 149.22382266666665;

		//[200.684407,149.712169],[201.526798,148.332516],[202.344842,149.626783]

		PFace startFace = triangulation.locate(x1, y1).get();

		assertTrue( "point location failed for (" + x + "," + y + ")",
						(triangulation.getMesh().isBorder(startFace) && !triangulation.getMesh().toPolygon(startFace).contains(x,y) ||
								!triangulation.getMesh().isBorder(startFace) && triangulation.getMesh().toPolygon(startFace).contains(x,y)));

		System.out.println(startFace);

		Optional<PFace> face = triangulation.locate(x, y, startFace);
		assertTrue( "point location failed for (" + x + "," + y + ")" + face.get(),
				face.isPresent() &&
						(triangulation.getMesh().isBorder(face.get()) && !triangulation.getMesh().toPolygon(face.get()).contains(x,y) ||
								!triangulation.getMesh().isBorder(face.get()) && triangulation.getMesh().toPolygon(face.get()).contains(x,y)));*/

	}

	@Test
	public void testJumpAndRunLocateAllVertices() {
		for(int i = 0; i < numberOfWalks; i++) {
			double x = triangulation.getMesh().getBound().getMinX() + random.nextDouble() * triangulation.getMesh().getBound().getWidth();
			double y = triangulation.getMesh().getBound().getMinY() + random.nextDouble() * triangulation.getMesh().getBound().getHeight();

			//System.out.println("point location for (" + x + "," + y +")" );

			Optional<PFace> face = triangulation.locateFace(new VPoint(x, y));

			//System.out.println(triangulation.getMesh().toPolygon(face.get()).contains(x,y));

			assertTrue( i + " point location failed for (" + x + "," + y + ")" + face.get(),
					face.isPresent() &&
							(triangulation.getMesh().isBorder(face.get()) && !triangulation.getMesh().toPolygon(face.get()).contains(x,y) ||
									!triangulation.getMesh().isBorder(face.get()) && triangulation.getMesh().toPolygon(face.get()).contains(x,y)));
		}

	}
}

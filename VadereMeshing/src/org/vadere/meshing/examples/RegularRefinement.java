package org.vadere.meshing.examples;

import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRegularRefinement;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRivaraRefinement;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.ADelaunayTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PDelaunayTriangulator;
import org.vadere.util.geometry.shapes.VPoint;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RegularRefinement {
	public static void main(String... args) throws InterruptedException, IOException {
		// (1) generate a random delaunay triangulation to work with
		Random random = new Random(10);
		int width = 10;
		int height = 10;
		int numberOfPoints = 100;
		Supplier<VPoint> supply = () -> new VPoint(random.nextDouble()*width, random.nextDouble()*height);
		Stream<VPoint> randomPoints = Stream.generate(supply);
		List<VPoint> points = randomPoints.limit(numberOfPoints).collect(Collectors.toList());

		// (2) compute the Delaunay triangulation
		var dt = new PDelaunayTriangulator(points);
		var triangulation = dt.generate();

		var meshImprover = new PEikMesh(
				p -> 2.0,
				dt.getTriangulation()
		);

		// display the mesh

		while (!meshImprover.isFinished()) {
			meshImprover.improve();
			//Thread.sleep(10);
		//	meshPanel.repaint();
		}

		meshImprover.finish();
		PMeshPanel meshPanel = new PMeshPanel(dt.getMesh().clone(), 800, 800);
		meshPanel.display("Random Delaunay triangulation");
		//meshPanel.repaint();

		GenRegularRefinement<PVertex, PHalfEdge, PFace> refinement = new GenRegularRefinement<>(triangulation, e -> triangulation.getMesh().toLine(e).length() > 0.5);

		//while (!refinement.isFinished()) {
			//Thread.sleep(2000);
			//synchronized (triangulation.getMesh()) {
				refinement.refine();
			//}
			//meshPanel.repaint();
		//}
		//meshPanel.repaint();

		PMeshPanel meshPanel3 = new PMeshPanel(dt.getMesh().clone(), 800, 800);
		meshPanel3.display("Refined mesh");

		//Thread.sleep(2000);

		refinement.coarse();

		PMeshPanel meshPanel2 = new PMeshPanel(dt.getMesh().clone(), 800, 800);
		meshPanel2.display("Coarsen mesh");


		//meshPanel.repaint();

		//Thread.sleep(2000);

	}

}

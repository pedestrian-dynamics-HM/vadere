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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
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
		MeshPanel meshPanel = new MeshPanel<>(meshImprover.getMesh(), 500, 500);
		meshPanel.display();
		while (!meshImprover.isFinished()) {
			meshImprover.improve();
			Thread.sleep(10);
			System.out.println("quality = " + meshImprover.getQuality());
			System.out.println("boundary edges = " + meshImprover.getMesh().getBoundaryEdges().size());
			meshPanel.repaint();
		}

		meshImprover.finish();
		meshPanel = new PMeshPanel(triangulation.getMesh(), 800, 800);
		meshPanel.display("Random Delaunay triangulation");
		//meshPanel.repaint();


		VPoint p = new VPoint(5,5);
		double radius = 2.0;

		GenRegularRefinement<PVertex, PHalfEdge, PFace> refinement = new GenRegularRefinement<>(
				triangulation,
				//e -> triangulation.getMesh().toLine(e).length() > 0.5 && triangulation.getMesh().toTriangle(triangulation.getMesh().getFace(e)).midPoint().distance(p) <= radius,
				3);

		Predicate<PHalfEdge> edgeSplitPredicate = e ->
				!triangulation.getMesh().isBoundary(e) &&
						triangulation.getMesh().toTriangle(triangulation.getMesh().getFace(e)).midPoint().distance(p) < 3.0 &&
						(!refinement.isGreen(e) || triangulation.getMesh().toLine(e).length() > 0.5);

		//refinement.setEdgeRefinementPredicate(edgeSplitPredicate);
		synchronized (triangulation.getMesh()) {
			refinement.refine();
		}

		meshPanel.repaint();

		/*Thread.sleep(2000);
		VPoint q = new VPoint(3,3);
		Predicate<PVertex> coarsePredicate = v -> q.distance(v) > 3.0;
		refinement.setCoarsePredicate(coarsePredicate);
		refinement.coarse();


		Thread.sleep(2000);

		edgeSplitPredicate = e ->
				!triangulation.getMesh().isBoundary(e) &&
						triangulation.getMesh().toTriangle(triangulation.getMesh().getFace(e)).midPoint().distance(q) < 3.0 &&
						(!refinement.isGreen(e) || triangulation.getMesh().toLine(e).length() > 0.5);
		refinement.setEdgeRefinementPredicate(edgeSplitPredicate);
		refinement.refine();*/
	}

}

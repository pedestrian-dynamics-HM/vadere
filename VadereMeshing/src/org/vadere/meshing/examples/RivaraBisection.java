package org.vadere.meshing.examples;

import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRivaraRefinement;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.ADelaunayTriangulator;
import org.vadere.util.geometry.shapes.VPoint;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RivaraBisection {
	public static void main(String... args) throws InterruptedException, IOException {
		// (1) generate a random delaunay triangulation to work with
		Random random = new Random(0);
		int width = 10;
		int height = 10;
		int numberOfPoints = 100;
		Supplier<VPoint> supply = () -> new VPoint(random.nextDouble()*width, random.nextDouble()*height);
		Stream<VPoint> randomPoints = Stream.generate(supply);
		List<VPoint> points = randomPoints.limit(numberOfPoints).collect(Collectors.toList());

		// (2) compute the Delaunay triangulation
		var delaunayTriangulator = new ADelaunayTriangulator(points);
		var triangulation = delaunayTriangulator.generate();

		GenRivaraRefinement<AVertex, AHalfEdge, AFace> refinement = new GenRivaraRefinement<>(triangulation, e -> triangulation.getMesh().toLine(e).length() > 0.3);
		MeshPanel<AVertex, AHalfEdge, AFace> panel = new MeshPanel<>(triangulation.getMesh(), 500, 500);
		panel.display();

		while (!refinement.isFinished()) {
			synchronized (triangulation.getMesh()) {
				refinement.refine();
			}
			panel.repaint();
			Thread.sleep(500);
		}
	}

}

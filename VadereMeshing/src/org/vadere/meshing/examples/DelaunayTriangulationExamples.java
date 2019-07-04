package org.vadere.meshing.examples;

import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRandomPointsSetTriangulator;
import org.vadere.util.geometry.shapes.VRectangle;

public class DelaunayTriangulationExamples {

	public static void main(String... args) {
		randomDelaunayTriangulation();
	}

	/**
	 * An example of how to construct and display a Delaunay triangulation of random points.
	 */
	public static void randomDelaunayTriangulation() {
		// define a bound of the mesh / triangulation
		VRectangle bound = new VRectangle(0, 0, 100, 100);

		// define a random point set triangulator
		int numberOfPoint = 5000;
		GenRandomPointsSetTriangulator randomTriangulator = new GenRandomPointsSetTriangulator(new PMesh(), numberOfPoint, bound);

		// fill in the points into the empty triangulation
		randomTriangulator.generate();

		// display the result
		MeshPanel meshPanel = new MeshPanel(randomTriangulator.getMesh(), 500, 500);
		meshPanel.display();
	}

}

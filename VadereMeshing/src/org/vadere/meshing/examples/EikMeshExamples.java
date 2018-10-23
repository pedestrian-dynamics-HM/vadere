package org.vadere.meshing.examples;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.PEikMesh;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

/**
 * Shows a very basic example how {@link org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMesh} can be used
 * to mesh a simple geometry.
 */
public class EikMeshExamples {

	public static void main(String... args) {

		// define a bounding box
		VPolygon boundary = GeometryUtils.polygonFromPoints2D(
				new VPoint(0,0),
				new VPoint(0, 1),
				new VPoint(1, 2),
				new VPoint(2,1),
				new VPoint(2,0));

		// define your holes
		VRectangle rect = new VRectangle(0.5, 0.5, 0.5, 0.5);
		List<VShape> obstacleShapes = new ArrayList<>();
		obstacleShapes.add(rect);

		// define the EikMesh-Improver
		PEikMesh meshImprover = new PEikMesh(
				boundary,
				0.1,
				obstacleShapes);

		// (optional) define the gui to display the mesh
		MeshPanel<EikMeshPoint, PVertex<EikMeshPoint>, PHalfEdge<EikMeshPoint>, PFace<EikMeshPoint>> meshPanel = new MeshPanel<>(
				meshImprover.getMesh(), 1000, 800,
				new VRectangle(boundary.getBounds()));

		// generate the mesh
		meshImprover.generate();

		// display the mesh
		meshPanel.display();
	}
}

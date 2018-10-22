package org.vadere.geometry.mesh.triangulation;

import org.vadere.geometry.GeometryUtils;
import org.vadere.geometry.mesh.gen.PFace;
import org.vadere.geometry.mesh.gen.PHalfEdge;
import org.vadere.geometry.mesh.gen.PVertex;
import org.vadere.geometry.mesh.triangulation.improver.EikMeshPanel;
import org.vadere.geometry.mesh.triangulation.improver.EikMeshPoint;
import org.vadere.geometry.mesh.triangulation.improver.PEikMesh;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VPolygon;
import org.vadere.geometry.shapes.VRectangle;
import org.vadere.geometry.shapes.VShape;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

public class Example {
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

		// (optional) define the gui only to display the mesh
		EikMeshPanel<EikMeshPoint, PVertex<EikMeshPoint>, PHalfEdge<EikMeshPoint>, PFace<EikMeshPoint>> eikMeshPanel = new EikMeshPanel<>(
				meshImprover.getMesh(),
				f -> false, 1000, 800,
				new VRectangle(boundary.getBounds()));
		JFrame frame = eikMeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// generate the mesh
		meshImprover.generate();
		eikMeshPanel.repaint();
	}
}

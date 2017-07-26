package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.MathUtil;
import org.vadere.util.tex.TexGraphGenerator;

import java.util.ArrayList;
import java.util.PriorityQueue;

import javax.swing.*;

/**
 * Created by Matimati-ka on 27.09.2016.
 */
public class TestEnhancedVersion3 extends JFrame {

	private TestEnhancedVersion3() {

		//IDistanceFunction distanceFunc1 = p -> 2 - Math.sqrt((p.getX()-1) * (p.getX()-1) + p.getY() * p.getY());
		//IDistanceFunction distanceFunc3 = p -> 2 - Math.sqrt((p.getX()-5) * (p.getX()-5) + p.getY() * p.getY());
		//IDistanceFunction distanceFunc = p -> -10+Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY());
		//IDistanceFunction distanceFunc = p -> 2 - Math.max(Math.abs(p.getX()-3), Math.abs(p.getY()));
		IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
		//IDistanceFunction distanceFunc4 = p -> Math.max(Math.abs(p.getY()) - 4, Math.abs(p.getX()) - 25);
		IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin();

		//IDistanceFunction distanceFunc = p -> Math.max(Math.max(Math.max(distanceFunc1.apply(p), distanceFunc2.apply(p)), distanceFunc3.apply(p)), distanceFunc4.apply(p));
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p))/2;
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin();
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.min(Math.abs(distanceFunc.apply(p) + 4), Math.abs(distanceFunc.apply(p)));
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
		VRectangle bbox = new VRectangle(-25, -15, 50, 30);
		PSMeshing meshGenerator = new PSMeshing(distanceFunc, edgeLengthFunc, 0.6, bbox, new ArrayList<>());
		meshGenerator.initialize();

		PSMeshingPanel distmeshPanel = new PSMeshingPanel(meshGenerator, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);


		//System.out.print(TexGraphGenerator.meshToGraph(meshGenerator.getMesh()));
		//double maxLen = meshGenerator.step();
		double avgQuality = 0.0;
		long obscuteTriangles = -1;
		int counter = 0;
		while (avgQuality <= 0.9318) {
			obscuteTriangles = meshGenerator.getTriangles().stream().filter(tri -> tri.isNonAcute()).count();
			PriorityQueue<PFace<MeshPoint>> priorityQueue = meshGenerator.getQuailties();
			avgQuality = priorityQueue.stream().reduce(0.0, (aDouble, meshPointPFace) -> aDouble + meshGenerator.faceToQuality(meshPointPFace), (d1, d2) -> d1 + d2) / priorityQueue.size();
			System.out.println("Average quality (" + counter + "):" + avgQuality);
			/*for(int i = 0; i < 100 && !priorityQueue.isEmpty(); i++) {
				PFace<MeshPoint> face = priorityQueue.poll();
				System.out.println("lowest quality ("+counter+"):"+ meshGenerator.faceToQuality(face));
			}*/
			distmeshPanel.update();
			distmeshPanel.repaint();
			counter++;
			meshGenerator.step();
		}
		//System.out.print("finished:" + meshGenerator.getMesh().getVertices().stream().filter(v -> !meshGenerator.getMesh().isDestroyed(v)).count());

		//System.out.print("finished:" + avgQuality);
		System.out.print(TexGraphGenerator.meshToGraph(meshGenerator.getMesh()));
		//if(counter == 1) {
		//
		//}
	}

	public static void main(String[] args) {
		new TestEnhancedVersion3();
	}
}

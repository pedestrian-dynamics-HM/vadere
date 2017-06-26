package org.vadere.util.triangulation.adaptive;

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

	private TestEnhancedVersion3()
	{

		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
		//IDistanceFunction distanceFunc = p -> -10+Math.Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY());
		//IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.max(Math.abs(p.getX()), Math.abs(p.getY()))) - 3;
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin();
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p));
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin();
		IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p));
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
		VRectangle bbox = new VRectangle(-11, -11, 22, 22);
		PSMeshing meshGenerator = new PSMeshing(distanceFunc, edgeLengthFunc, 0.6, bbox, new ArrayList<>());
		meshGenerator.initialize();

		PSMeshingPanel distmeshPanel = new PSMeshingPanel(meshGenerator, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);


		//System.out.print(TexGraphGenerator.meshToGraph(meshGenerator.getMesh()));
		//double maxLen = meshGenerator.step();
		double avgQuality = 0.0;
		int counter = 0;
		while (avgQuality < 0.96) {
			PriorityQueue<PFace<MeshPoint>> priorityQueue = meshGenerator.getQuailties();
			avgQuality =  priorityQueue.stream().reduce(0.0, (aDouble, meshPointPFace) -> aDouble + meshGenerator.faceToQuality(meshPointPFace), (d1, d2) -> d1 + d2) / priorityQueue.size();
			System.out.println("Average quality ("+counter+"):" + avgQuality);
			for(int i = 0; i < 100 && !priorityQueue.isEmpty(); i++) {
				PFace<MeshPoint> face = priorityQueue.poll();
				System.out.println("lowest quality ("+counter+"):"+ meshGenerator.faceToQuality(face));
			}
			distmeshPanel.update();
			distmeshPanel.repaint();
			counter++;
			meshGenerator.step();
		}

		System.out.print("finished:" + avgQuality);

		//if(counter == 1) {
		//	System.out.print(TexGraphGenerator.meshToGraph(meshGenerator.getMesh()));
		//}
	}

	public static void main(String[] args) {
		new TestEnhancedVersion3();
	}
}

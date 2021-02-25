package org.vadere.meshing.mesh.triangulation.plots;

import org.vadere.util.math.IDistanceFunction;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.triangulation.improver.LaplacianSmother;

import javax.swing.*;
import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * @author Benedikt Zoennchen
 */
public class CPULaplacianSmoothing extends JFrame {

    private CPULaplacianSmoothing() {

        //IDistanceFunction distanceFunc1 = p -> 2 - Math.sqrt((p.getX()-1) * (p.getX()-1) + p.getY() * p.getY());
        //IDistanceFunction distanceFunc3 = p -> 2 - Math.sqrt((p.getX()-5) * (p.getX()-5) + p.getY() * p.getY());
        //IDistanceFunction distanceFunc = p -> -10+Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY());
        //IDistanceFunction distanceFunc = p -> 2 - Math.max(Math.abs(p.getX()-3), Math.abs(p.getY()));
        IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
        //IDistanceFunction distanceFunc4 = p -> Math.max(Math.abs(p.getY()) - 4, Math.abs(p.getX()) - 25);
        IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin() / 10;
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.min(Math.abs(distanceFunc.apply(p) + 4), Math.abs(distanceFunc.apply(p)));
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p)*0.5);


        //IDistanceFunction distanceFunc = p -> Math.max(Math.max(Math.max(distanceFunc1.apply(p), distanceFunc2.apply(p)), distanceFunc3.apply(p)), distanceFunc4.apply(p));
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p))/2;
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin();
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.min(Math.abs(distanceFunc.apply(p) + 4), Math.abs(distanceFunc.apply(p)));
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
        VRectangle bbox = new VRectangle(-11, -11, 22, 22);
        LaplacianSmother meshGenerator = new LaplacianSmother(distanceFunc, edgeLengthFunc, 0.5, bbox, new ArrayList<>());


        Predicate<PFace> predicate = face -> meshGenerator.getTriangulation().getMesh().toTriangle(face).isNonAcute();
        MeshPanel<PVertex, PHalfEdge, PFace> distmeshPanel = new MeshPanel(meshGenerator.getMesh(), predicate, 1000, 800);

        JFrame frame = distmeshPanel.display();
        frame.setVisible(true);


		//System.out.print(TexGraphGenerator.meshToGraph(meshGenerator.getMesh()));
		//double maxLen = meshGenerator.step();
		double avgQuality = 0.0;
		long obscuteTriangles = -1;
		int counter = 0;
		long time = 0;

        while (counter <= 2000) {
            //obscuteTriangles = meshGenerator.getTriangles().stream().filter(tri -> tri.isNonAcute()).count();
            //PriorityQueue<PFace<MeshPoint>> priorityQueue = meshGenerator.getQuailties();
            //avgQuality = priorityQueue.stream().reduce(0.0, (aDouble, meshPointPFace) -> aDouble + meshGenerator.faceToQuality(meshPointPFace), (d1, d2) -> d1 + d2) / priorityQueue.size();
            //System.out.println("Average quality (" + counter + "):" + avgQuality);
			/*for(int i = 0; i < 100 && !priorityQueue.isEmpty(); i++) {
				PFace<MeshPoint> face = priorityQueue.poll();
				System.out.println("lowest quality ("+counter+"):"+ meshGenerator.faceToQuality(face));
			}*/
            distmeshPanel.repaint();
            counter++;

			long ms = System.currentTimeMillis();
			meshGenerator.improve();
			ms = System.currentTimeMillis() - ms;
			time += ms;
			//System.out.println("Quality: " + meshGenerator.getQuality());
			System.out.println("Step-Time: " + ms);
		}
		//meshGenerator.finish();
		System.out.print("overall time: " + time);
        //System.out.print(TexGraphGenerator.meshToGraph(meshGenerator.getMesh()));
		//System.out.print("finished:" + meshGenerator.getMesh().getVertices().stream().filter(v -> !meshGenerator.getMesh().isDestroyed(v)).count());

		//System.out.print("finished:" + avgQuality);
		//System.out.print(TexGraphGenerator.meshToGraph(meshGenerator.getMesh()));
		//if(counter == 1) {
		//
		//}
	}

    public static void main(String[] args) {
        new CPULaplacianSmoothing();
    }
}

package org.vadere.util.triangulation.adaptive;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.triangulation.improver.PSMeshing;

import java.util.ArrayList;
import java.util.function.Predicate;

import javax.swing.*;

/**
 * Created by Matimati-ka on 27.09.2016.
 */
public class TestEnhancedVersion3 extends JFrame {

    private static final Logger log = LogManager.getLogger(TestEnhancedVersion3.class);

    private TestEnhancedVersion3() {

        //IDistanceFunction distanceFunc1 = p -> 2 - Math.sqrt((p.getX()-1) * (p.getX()-1) + p.getY() * p.getY());
        //IDistanceFunction distanceFunc3 = p -> 2 - Math.sqrt((p.getX()-5) * (p.getX()-5) + p.getY() * p.getY());
        //IDistanceFunction distanceFunc = p -> -10+Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY());
        //IDistanceFunction distanceFunc = p -> 2 - Math.max(Math.abs(p.getX()-3), Math.abs(p.getY()));
        IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
        //IDistanceFunction distanceFunc4 = p -> Math.max(Math.abs(p.getY()) - 4, Math.abs(p.getX()) - 25);
        IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.min(Math.abs(distanceFunc.apply(p) + 4), Math.abs(distanceFunc.apply(p)));
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p)*0.5);


        //IDistanceFunction distanceFunc = p -> Math.max(Math.max(Math.max(distanceFunc1.apply(p), distanceFunc2.apply(p)), distanceFunc3.apply(p)), distanceFunc4.apply(p));
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p))/2;
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin();
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.min(Math.abs(distanceFunc.apply(p) + 4), Math.abs(distanceFunc.apply(p)));
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
        VRectangle bbox = new VRectangle(-11, -11, 22, 22);
        PSMeshing meshGenerator = new PSMeshing(distanceFunc, edgeLengthFunc, 2.0, bbox, new ArrayList<>());

        Predicate<PFace<MeshPoint>> predicate = face -> !meshGenerator.getTriangulation().isCCW(face);

		PSMeshingPanel<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> distmeshPanel = new PSMeshingPanel(meshGenerator, predicate, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);


		//System.out.print(TexGraphGenerator.meshToGraph(meshGenerator.getMesh()));
		//double maxLen = meshGenerator.step();
		double avgQuality = 0.0;
		long obscuteTriangles = -1;
		int counter = 0;
		long time = 0;

        StopWatch overAllTime = new StopWatch();
        overAllTime.start();
        while (counter <= 300) {
            meshGenerator.improve();
            overAllTime.suspend();

            distmeshPanel.repaint();
            counter++;
            System.out.println("Quality: " + meshGenerator.getQuality());
            overAllTime.resume();
        }
        overAllTime.stop();
        log.info("#vertices:" + meshGenerator.getMesh().getVertices().size());
        log.info("#edges:" + meshGenerator.getMesh().getEdges().size());
        log.info("overall time: " + overAllTime.getTime());
	}

    public static void main(String[] args) {
        new TestEnhancedVersion3();
    }
}

package org.vadere.util.triangulation.adaptive.plots;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AHalfEdge;
import org.vadere.util.geometry.mesh.gen.AMesh;
import org.vadere.util.geometry.mesh.gen.AVertex;
import org.vadere.util.geometry.mesh.inter.IMeshSupplier;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.triangulation.IPointConstructor;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;
import org.vadere.util.triangulation.adaptive.MeshPoint;
import org.vadere.util.triangulation.adaptive.PSMeshingPanel;
import org.vadere.util.triangulation.improver.PSMeshing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.*;

/**
 * @author Benedikt Zoennchen
 */
public class RunTimeCPU extends JFrame {

    private static final Logger log = LogManager.getLogger(RunTimeCPU.class);

    private RunTimeCPU() {

	    VRectangle bbox = new VRectangle(-11, -11, 22, 22);

	    //IDistanceFunction distanceFunc1 = p -> 2 - Math.sqrt((p.getX()-1) * (p.getX()-1) + p.getY() * p.getY());
        //IDistanceFunction distanceFunc3 = p -> 2 - Math.sqrt((p.getX()-5) * (p.getX()-5) + p.getY() * p.getY());
        //IDistanceFunction distanceFunc = p -> -10+Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY());
        //IDistanceFunction distanceFunc = p -> 2 - Math.max(Math.abs(p.getX()-3), Math.abs(p.getY()));


	    //IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
	    //IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.max(Math.abs(p.getX()), Math.abs(p.getY()))) - 3;
	    VRectangle rect = new VRectangle(-4, -4, 8, 8);
	    VPolygon hexagon = VShape.generateHexagon(4.0);
	    //IDistanceFunction distanceFunc = IDistanceFunction.create(bbox, rect);

	    //IDistanceFunction distanceFunc = p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 10;
		IDistanceFunction distanceFunc = IDistanceFunction.intersect(p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 10, IDistanceFunction.create(bbox, rect));

	    List<VShape> obstacles = new ArrayList<>();
	    //obstacles.add(rect);

	    obstacles.add(rect);

	    //IDistanceFunction distanceFunc4 = p -> Math.max(Math.abs(p.getY()) - 4, Math.abs(p.getX()) - 25);
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
        IEdgeLengthFunction edgeLengthFunc = p -> 0.7;
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.min(Math.abs(distanceFunc.apply(p) + 4), Math.abs(distanceFunc.apply(p)));
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p)*0.5);


        //IDistanceFunction distanceFunc = p -> Math.max(Math.max(Math.max(distanceFunc1.apply(p), distanceFunc2.apply(p)), distanceFunc3.apply(p)), distanceFunc4.apply(p));
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p))/2;
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin();
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.min(Math.abs(distanceFunc.apply(p) + 4), Math.abs(distanceFunc.apply(p)));
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0;


	    IPointConstructor<MeshPoint> pointConstructor = (x, y) -> new MeshPoint(x, y, false);
        IMeshSupplier<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> supplier = () -> new AMesh<>(pointConstructor);

        PSMeshing<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> meshGenerator = new PSMeshing<>(
                distanceFunc,
                edgeLengthFunc,
                2.0,
                bbox, obstacles,
                supplier);

        //Predicate<AFace<MeshPoint>> predicate = face -> !meshGenerator.getTriangulation().isCCW(face);
	    Predicate<AFace<MeshPoint>> predicate = face -> meshGenerator.getTriangulation().getMesh().isHole(face);

		PSMeshingPanel<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> distmeshPanel = new PSMeshingPanel(meshGenerator.getMesh(), predicate, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("CPU");


		//System.out.print(TexGraphGenerator.meshToGraph(meshGenerator.getMesh()));
		//double maxLen = meshGenerator.step();
		double avgQuality = 0.0;
		long obscuteTriangles = -1;
		int counter = 0;
		long time = 0;

        StopWatch overAllTime = new StopWatch();
        overAllTime.start();
        while (counter < 200) {
            meshGenerator.improve();
            overAllTime.suspend();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            distmeshPanel.repaint();
            counter++;
            //System.out.println("Quality: " + meshGenerator.getQuality());
            overAllTime.resume();
        }
        overAllTime.stop();
        log.info("#vertices:" + meshGenerator.getMesh().getVertices().size());
        log.info("#edges:" + meshGenerator.getMesh().getEdges().size());
        log.info("overall time: " + overAllTime.getTime());
	}

    public static void main(String[] args) {
        new RunTimeCPU();
    }
}

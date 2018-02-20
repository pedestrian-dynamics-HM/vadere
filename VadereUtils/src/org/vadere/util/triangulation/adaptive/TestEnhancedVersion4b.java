package org.vadere.util.triangulation.adaptive;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AHalfEdge;
import org.vadere.util.geometry.mesh.gen.AMesh;
import org.vadere.util.geometry.mesh.gen.AVertex;
import org.vadere.util.geometry.mesh.inter.IMeshSupplier;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.triangulation.IPointConstructor;
import org.vadere.util.triangulation.ITriangulationSupplier;

import javax.swing.*;
import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * Created by Matimati-ka on 27.09.2016.
 */
public class TestEnhancedVersion4b extends JFrame {

	private static final Logger log = LogManager.getLogger(TestEnhancedVersion4b.class);

    private TestEnhancedVersion4b() {

		//IDistanceFunction distanceFunc1 = p -> 2 - Math.sqrt((p.getX()-1) * (p.getX()-1) + p.getY() * p.getY());
		//IDistanceFunction distanceFunc3 = p -> 2 - Math.sqrt((p.getX()-5) * (p.getX()-5) + p.getY() * p.getY());
		//IDistanceFunction distanceFunc = p -> -10+Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY());
		//IDistanceFunction distanceFunc = p -> 2 - Math.max(Math.abs(p.getX()-3), Math.abs(p.getY()));
		IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
		//IDistanceFunction distanceFunc4 = p -> Math.max(Math.abs(p.getY()) - 4, Math.abs(p.getX()) - 25);
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin();
		IEdgeLengthFunction edgeLengthFunc = p -> 0.3;

		//IDistanceFunction distanceFunc = p -> Math.max(Math.max(Math.max(distanceFunc1.apply(p), distanceFunc2.apply(p)), distanceFunc3.apply(p)), distanceFunc4.apply(p));
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p))/2;
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin();
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.min(Math.abs(distanceFunc.apply(p) + 4), Math.abs(distanceFunc.apply(p)));
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
		VRectangle bbox = new VRectangle(-11, -11, 22, 22);

	    IPointConstructor<MeshPoint> pointConstructor = (x, y) -> new MeshPoint(x, y, false);
	    IMeshSupplier<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> supplier = () -> new AMesh<>(pointConstructor);


	    CLPSMeshingHE meshGenerator = new CLPSMeshingHE(distanceFunc, edgeLengthFunc, 1.0, bbox, new ArrayList<>(), supplier);
		meshGenerator.initialize();
		Predicate<AFace<MeshPoint>> predicate = face -> !meshGenerator.getTriangulation().isCCW(face);
		PSMeshingPanel distmeshPanel = new PSMeshingPanel(meshGenerator.getMesh(), predicate, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("GPU");

		log.info("number of points: " + meshGenerator.getMesh().getNumberOfVertices());

		int counter = 0;
		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		while (counter < 1) {
			boolean retriangulation = meshGenerator.step(true);
			overAllTime.suspend();

			meshGenerator.refresh();
			distmeshPanel.repaint();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			counter++;

			overAllTime.resume();
		}
		overAllTime.stop();

		meshGenerator.finish();
		distmeshPanel.repaint();
		log.info("#vertices:" + meshGenerator.getMesh().getVertices().size());
		log.info("#edges:" + meshGenerator.getMesh().getEdges().size());
		log.info("overall time: " + overAllTime.getTime());
	}

    public static void main(String[] args) {
        new TestEnhancedVersion4b();
    }
}
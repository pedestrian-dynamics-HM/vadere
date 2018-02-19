package org.vadere.util.triangulation.adaptive;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.*;
import org.vadere.util.geometry.mesh.inter.IMeshSupplier;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.triangulation.IPointConstructor;
import org.vadere.util.triangulation.ITriangulationSupplier;
import org.vadere.util.triangulation.triangulator.UniformRefinementTriangulatorCFS;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bzoennchen on 30.01.18.
 */
public class TestUniTriangulation extends JFrame {


    private static final Logger log = LogManager.getLogger(TestUniTriangulation.class);

    private TestUniTriangulation() {

        IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
        VRectangle bbox = new VRectangle(-11, -11, 22, 22);

	    IPointConstructor<VPoint> pointConstructor = (x, y) -> new VPoint(x, y);
	    IMeshSupplier<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> supplier = () -> new AMesh<>(pointConstructor);

        UniformRefinementTriangulatorCFS<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> uniformRefinementTriangulation =
                new UniformRefinementTriangulatorCFS<>(supplier, bbox, new ArrayList<>(), p -> 1.0, distanceFunc);

	    ITriangulation<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> triangulation = uniformRefinementTriangulation.init();
        PSMeshingPanel<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> distmeshPanel =
                new PSMeshingPanel<>(triangulation.getMesh(), f -> triangulation.getMesh().isHole(f), 1000, 800, bbox);
        JFrame frame = distmeshPanel.display();
        frame.setVisible(true);
        frame.setTitle("CPU");



        double avgQuality = 0.0;
        long obscuteTriangles = -1;
        int counter = 0;
        long time = 0;

        StopWatch overAllTime = new StopWatch();
        overAllTime.start();

        while (!uniformRefinementTriangulation.isFinished()) {

            //meshGenerator.improve();
            overAllTime.suspend();
           /* try {
                Thread.sleep(10);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
	        uniformRefinementTriangulation.step();
            //distmeshPanel.repaint();
            counter++;
            //System.out.println("Quality: " + meshGenerator.getQuality());
            overAllTime.resume();

            boolean removedSome = true;
        }

	    overAllTime.stop();
	    log.info("rdy");

	    counter = 0;
        while (counter < 1) {
        	counter++;
	        try {
	        	uniformRefinementTriangulation.finish();

		        Thread.sleep(1);

	        } catch (InterruptedException e) {
		        e.printStackTrace();
	        }
        }
	    distmeshPanel.repaint();
        //uniformRefinementTriangulation.finish();



    }

    public static void main(String[] args) {
        new TestUniTriangulation();
    }

}

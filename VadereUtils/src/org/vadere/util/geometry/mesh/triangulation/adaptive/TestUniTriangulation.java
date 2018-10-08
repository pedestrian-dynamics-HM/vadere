package org.vadere.util.geometry.mesh.triangulation.adaptive;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.*;
import org.vadere.util.geometry.mesh.inter.IMeshSupplier;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.mesh.triangulation.improver.EikMeshPanel;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.mesh.inter.IPointConstructor;
import org.vadere.util.geometry.mesh.triangulation.triangulator.UniformRefinementTriangulatorSFC;

import javax.swing.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.function.Function;

/**
 * This class is for testing and to give an example of how to use {@link UniformRefinementTriangulatorSFC}.
 *
 * @author Benedikt Zoennchen
 */
public class TestUniTriangulation extends JFrame {


    private static final Logger log = LogManager.getLogger(TestUniTriangulation.class);

    private TestUniTriangulation() {

    	// the distance function defining the geometry
        IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;

        // a bounding box which contains the geometry
        VRectangle bbox = new VRectangle(-11, -11, 22, 22);

        // a point constructor for immutable points
	    IPointConstructor<VPoint> pointConstructor = (x, y) -> new VPoint(x, y);

	    // a mesh supplier for a default mesh
	    IMeshSupplier<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> supplier = () -> new AMesh<>(pointConstructor);

	    // the mesh refinement triangulator
        UniformRefinementTriangulatorSFC<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> uniformRefinementTriangulation =
                new UniformRefinementTriangulatorSFC<>(supplier, bbox, new ArrayList<>(), p -> 0.1, 1.5, distanceFunc, new ArrayList<>());

        // to measure the time consumption
	    StopWatch overAllTime = new StopWatch();

	    /*
	     * GUI-Code
	     */
	    ITriangulation<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> triangulation = uniformRefinementTriangulation.init();
	    Function<AFace<VPoint>, Color> colorFunction = f -> new Color(Color.HSBtoRGB((float)(f.getId() / (1.0f * triangulation.getMesh().getNumberOfFaces())), 1f, 0.75f));
        EikMeshPanel<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> meshPanel =
                new EikMeshPanel<>(triangulation.getMesh(), f -> triangulation.getMesh().isHole(f), 1000, 800, bbox, colorFunction);
        JFrame frame = meshPanel.display();
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setTitle("Test-UniformRefinementTriangulatorSFC");
	    /*
	     * GUI-Code end
	     */

        overAllTime.start();
	    overAllTime.suspend();
        while (!uniformRefinementTriangulation.isFinished()) {
            try {
                Thread.sleep(500);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
	        overAllTime.resume();
	        uniformRefinementTriangulation.step();
	        overAllTime.suspend();

	        log.info("computation time = " + (overAllTime.getTime()) + "[ms]");
            meshPanel.repaint();
        }
	    overAllTime.resume();
	    overAllTime.stop();

		log.info("computation time = " + (overAllTime.getTime()) + "[ms]");
    }

    public static void main(String[] args) {
        new TestUniTriangulation();
    }

}

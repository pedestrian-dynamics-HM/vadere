package org.vadere.geometry.triangulation;

import org.apache.commons.lang3.time.StopWatch;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenUniformRefinementTriangulatorSFC;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import javax.swing.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.function.Function;

/**
 * This class is for testing and to give an example of how to use {@link GenUniformRefinementTriangulatorSFC}.
 *
 * @author Benedikt Zoennchen
 */
public class TestUniTriangulation extends JFrame {


    private static final Logger log = Logger.getLogger(TestUniTriangulation.class);

    private TestUniTriangulation() {

    	// the distance function defining the geometry
        IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;

        // a bounding box which contains the geometry
        VRectangle bbox = new VRectangle(-11, -11, 22, 22);

        // a point constructor for immutable points
	    IPointConstructor<VPoint> pointConstructor = (x, y) -> new VPoint(x, y);

	    // a mesh supplier for a default mesh
	    IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();

	    // the mesh refinement triangulator
        GenUniformRefinementTriangulatorSFC<AVertex, AHalfEdge, AFace> uniformRefinementTriangulation =
                new GenUniformRefinementTriangulatorSFC<>(supplier, bbox, new ArrayList<>(), p -> 0.15, distanceFunc);

        // to measure the time consumption
	    StopWatch overAllTime = new StopWatch();

	    /*
	     * GUI-Code
	     */
	    IIncrementalTriangulation<AVertex, AHalfEdge, AFace> triangulation = uniformRefinementTriangulation.init();
	    Function<AFace, Color> colorFunction = f -> new Color(Color.HSBtoRGB((float)(f.getId() / (1.0f * triangulation.getMesh().getNumberOfFaces())), 1f, 0.75f));
        MeshPanel<AVertex, AHalfEdge, AFace> meshPanel =
                new MeshPanel<>(triangulation.getMesh(), f -> triangulation.getMesh().isHole(f), 1000, 800, colorFunction);
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
	        uniformRefinementTriangulation.refine();
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

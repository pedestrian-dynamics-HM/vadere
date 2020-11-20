package org.vadere.simulator.models.potential.solver;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.calculators.mesh.PotentialPoint;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.util.data.cellgrid.IPotentialPoint;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TestFFMUniformTriangulation {

    private static Logger log = Logger.getLogger(TestFFMUniformTriangulation.class);

    private IIncrementalTriangulation<PVertex, PHalfEdge, PFace> uniformTriangulation;
    private int width = 10;
    private int height = 10;
    private double minTriangleSideLength = 1.0;

    @Before
    public void setUp() throws Exception {
        IPointConstructor<IPotentialPoint> pointConstructor = (x, y) -> new PotentialPoint(x, y);
        uniformTriangulation = IIncrementalTriangulation.createUniformTriangulation(
                IPointLocator.Type.BASE,
                new VRectangle(0, 0, width, height),
                minTriangleSideLength
        );
    }

	@Ignore
    @Test
    public void testFFM() {
        List<IPoint> targetPoints = new ArrayList<>();
        targetPoints.add(new EikMeshPoint(5,5, false));
        //EikonalSolver solver = new EikonalSolverFMMAcuteTriangulation(targetPoints, new UnitTimeCostFunction(), uniformTriangulation);

		/*MeshPanel panel = new MeshPanel(uniformTriangulation.getMesh(), 500, 500, new VRectangle(0, 0, width, height));
		panel.display();

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
        EikonalSolver solver = new MeshEikonalSolverFMM(new UnitTimeCostFunction(), targetPoints, uniformTriangulation);
        log.info("start FFM");
        solver.solve();
        log.info("FFM finished");
        try {
            //System.out.println(getClass().getClassLoader().getResource("./potentialField.csv").getFile());
            FileWriter writer = new FileWriter("./potentialField.csv");
            for(double y = 0.2; y < height-0.2; y += 0.1) {
                for(double x = 0.2; x < width-0.2; x += 0.1) {
                    writer.write(""+solver.getPotential(x ,y) + " ");
                }
                writer.write("\n");
            }
            writer.flush();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertTrue(0.1 > solver.getPotential(5, 5));
        assertTrue(0.0 < solver.getPotential(1, 7));
    }
}

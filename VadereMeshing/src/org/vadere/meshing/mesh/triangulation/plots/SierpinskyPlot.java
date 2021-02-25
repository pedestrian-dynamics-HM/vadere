package org.vadere.meshing.mesh.triangulation.plots;

import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenUniformRefinementTriangulatorSFC;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

/**
 * Created by bzoennchen on 15.03.18.
 */
public class SierpinskyPlot {
	private static final Logger log = Logger.getLogger(RunTimeCPU.class);

	/**
	 * Each geometry is contained this bounding box.
	 */
	private static final VRectangle bbox = new VRectangle(-11, -11, 22, 22);
	private static IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y, false);
	private static double initialEdgeLength = 0.3;

	/**
	 * A circle with radius 10.0 meshed using a uniform mesh.
	 */
	private static void uniformCircle(final double initialEdgeLength) {
		IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();
		IDistanceFunction distanceFunc = p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 10;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + (Math.abs(distanceFunc.apply(p)) * Math.abs(distanceFunc.apply(p)));

		GenUniformRefinementTriangulatorSFC<AVertex, AHalfEdge, AFace> uniformRefinementTriangulation = new GenUniformRefinementTriangulatorSFC(
				supplier,
				bbox,
				obstacles,
				edgeLengthFunc,
				distanceFunc);

		IIncrementalTriangulation<AVertex, AHalfEdge, AFace> triangulation = uniformRefinementTriangulation.init();
		MeshPanel<AVertex, AHalfEdge, AFace> panel = new MeshPanel<>(triangulation.getMesh(), f -> false, 1000, 800);
		JFrame frame = panel.display();
		frame.setVisible(true);

		while (!uniformRefinementTriangulation.isFinished()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			uniformRefinementTriangulation.refine();
			log.info("step");
			panel.repaint();
		}

		log.info("end");

	}

	public static void main(String[] args) {
		uniformCircle(initialEdgeLength );
	}
}

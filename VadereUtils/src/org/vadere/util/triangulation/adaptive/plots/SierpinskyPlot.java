package org.vadere.util.triangulation.adaptive.plots;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AHalfEdge;
import org.vadere.util.geometry.mesh.gen.AMesh;
import org.vadere.util.geometry.mesh.gen.AVertex;
import org.vadere.util.geometry.mesh.inter.IMeshSupplier;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.triangulation.IPointConstructor;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;
import org.vadere.util.triangulation.adaptive.MeshPoint;
import org.vadere.util.triangulation.adaptive.PSMeshingPanel;
import org.vadere.util.triangulation.triangulator.UniformRefinementTriangulatorSFC;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

/**
 * Created by bzoennchen on 15.03.18.
 */
public class SierpinskyPlot {
	private static final Logger log = LogManager.getLogger(RunTimeCPU.class);

	/**
	 * Each geometry is contained this bounding box.
	 */
	private static final VRectangle bbox = new VRectangle(-11, -11, 22, 22);
	private static IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static IPointConstructor<MeshPoint> pointConstructor = (x, y) -> new MeshPoint(x, y, false);
	private static double initialEdgeLength = 1.0;

	/**
	 * A circle with radius 10.0 meshed using a uniform mesh.
	 */
	private static void uniformCircle(final double initialEdgeLength) {
		IMeshSupplier<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 10;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p));

		UniformRefinementTriangulatorSFC<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> uniformRefinementTriangulation = new UniformRefinementTriangulatorSFC(
				supplier,
				bbox,
				obstacles,
				p -> edgeLengthFunc.apply(p) * initialEdgeLength,
				distanceFunc,
				new ArrayList<>());

		ITriangulation<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> triangulation = uniformRefinementTriangulation.init();
		PSMeshingPanel<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> panel = new PSMeshingPanel<>(triangulation.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = panel.display();
		frame.setVisible(true);

		while (!uniformRefinementTriangulation.isFinished()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			uniformRefinementTriangulation.step();
			log.info("step");
			panel.repaint();
		}

		log.info("end");

	}

	public static void main(String[] args) {
		uniformCircle(initialEdgeLength );
	}
}

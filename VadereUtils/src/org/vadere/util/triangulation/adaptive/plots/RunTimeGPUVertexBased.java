package org.vadere.util.triangulation.adaptive.plots;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AHalfEdge;
import org.vadere.util.geometry.mesh.gen.AMesh;
import org.vadere.util.geometry.mesh.gen.AVertex;
import org.vadere.util.geometry.mesh.inter.IMeshSupplier;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.triangulation.IPointConstructor;
import org.vadere.util.triangulation.adaptive.CLPSMeshing;
import org.vadere.util.triangulation.adaptive.CLPSMeshingHE;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;
import org.vadere.util.triangulation.adaptive.MeshPoint;
import org.vadere.util.triangulation.adaptive.PSMeshingPanel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Benedikt Zoennchen
 */
public class RunTimeGPUVertexBased extends JFrame {

	private static final Logger log = LogManager.getLogger(RunTimeGPUVertexBased.class);
	private static final VRectangle bbox = new VRectangle(-11, -11, 22, 22);
	private static final IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static final IPointConstructor<MeshPoint> pointConstructor = (x, y) -> new MeshPoint(x, y, false);
	private static final double initialEdgeLength = 1.5;

	private static void overallUniformRing() {

		IMeshSupplier<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = 0.5;
		double minInitialEdgeLength = 0.5;


		while (initialEdgeLength >= minInitialEdgeLength) {
			//CLPSMeshing meshGenerator = new CLPSMeshing(distanceFunc, uniformEdgeLength, initialEdgeLength, bbox, new ArrayList<>(), supplier);
			CLPSMeshingHE meshGenerator = new CLPSMeshingHE(distanceFunc, uniformEdgeLength, initialEdgeLength, bbox, new ArrayList<>(), supplier);

			StopWatch overAllTime = new StopWatch();
			overAllTime.start();
			meshGenerator.generate();
			overAllTime.stop();

			log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
			log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
			log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
			log.info("quality" + meshGenerator.getQuality());
			log.info("overall time: " + overAllTime.getTime() + "[ms]");

			PSMeshingPanel<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> distmeshPanel = new PSMeshingPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
			JFrame frame = distmeshPanel.display();
			frame.setVisible(true);
			frame.setTitle("uniformRing()");
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			distmeshPanel.repaint();

			initialEdgeLength = initialEdgeLength * 0.5;
		}
	}

	public static void main(String[] args) {
		overallUniformRing();
	}
}

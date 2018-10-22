package org.vadere.geometry.mesh.triangulation.plots;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.geometry.IDistanceFunction;
import org.vadere.geometry.mesh.gen.AFace;
import org.vadere.geometry.mesh.gen.AHalfEdge;
import org.vadere.geometry.mesh.gen.AMesh;
import org.vadere.geometry.mesh.gen.AVertex;
import org.vadere.geometry.mesh.inter.IMeshSupplier;
import org.vadere.geometry.opencl.OpenCLException;
import org.vadere.geometry.shapes.VRectangle;
import org.vadere.geometry.shapes.VShape;
import org.vadere.geometry.mesh.inter.IPointConstructor;
import org.vadere.geometry.mesh.triangulation.improver.opencl.CLPSMeshingHE;
import org.vadere.geometry.mesh.triangulation.adaptive.IEdgeLengthFunction;
import org.vadere.geometry.mesh.triangulation.improver.EikMeshPoint;
import org.vadere.geometry.mesh.triangulation.improver.EikMeshPanel;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

public class VisualTestGPUVertexBased {
	private static final Logger log = LogManager.getLogger(RunTimeGPUEdgeBased.class);

	private static final VRectangle bbox = new VRectangle(-11, -11, 22, 22);
	private static final IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static final IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y, false);
	private static final double initialEdgeLength =  0.5;

	private static void overallUniformRing() throws OpenCLException {

		IMeshSupplier<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();

		CLPSMeshingHE meshGenerator = new CLPSMeshingHE(distanceFunc, uniformEdgeLength, initialEdgeLength, bbox, new ArrayList<>(), supplier);
		meshGenerator.initialize();

		EikMeshPanel<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> distmeshPanel = new EikMeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		overAllTime.suspend();
		int nSteps = 0;
		while (nSteps < 300) {
			nSteps++;
			overAllTime.resume();
			meshGenerator.improve();
			overAllTime.suspend();
			meshGenerator.refresh();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			distmeshPanel.repaint();
		}
		overAllTime.stop();
		meshGenerator.finish();

		log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
		log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
		log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");
	}

	public static void main(String[] args) throws OpenCLException {
		overallUniformRing();
	}
}

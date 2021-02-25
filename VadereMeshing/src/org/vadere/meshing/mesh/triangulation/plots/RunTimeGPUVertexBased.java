package org.vadere.meshing.mesh.triangulation.plots;

import org.apache.commons.lang3.time.StopWatch;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.opencl.CLEikMeshHE;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.opencl.OpenCLException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

/**
 * @author Benedikt Zoennchen
 */
public class RunTimeGPUVertexBased extends JFrame {

	private static final Logger log = Logger.getLogger(RunTimeGPUEdgeBased.class);

	private static final VRectangle bbox = new VRectangle(-11, -11, 22, 22);
	private static final IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static final IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y, false);
	private static final double initialEdgeLength = 1.5;

	private static void overallUniformRing() {

		IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = 2.0;
		double minInitialEdgeLength = 0.7;


		while (initialEdgeLength >= minInitialEdgeLength) {
			CLEikMeshHE meshGenerator = new CLEikMeshHE(distanceFunc, uniformEdgeLength, initialEdgeLength, bbox, new ArrayList<>(), supplier);

			StopWatch overAllTime = new StopWatch();
			overAllTime.start();
			meshGenerator.generate();
			overAllTime.stop();
			meshGenerator.refresh();

			log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
			log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
			log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
			log.info("quality" + meshGenerator.getQuality());
			log.info("overall time: " + overAllTime.getTime() + "[ms]");

			MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800);
			JFrame frame = distmeshPanel.display();
			frame.setVisible(true);
			frame.setTitle("uniformRing()");
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			distmeshPanel.repaint();

			initialEdgeLength = initialEdgeLength - 0.05;
		}
	}

	private static void stepUniformRing(double startLen, double endLen, double stepLen) throws OpenCLException {
		IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();

		//double initialEdgeLength = 0.125;
		//double minInitialEdgeLength = 0.05;

		double initialEdgeLength = startLen;
		double minInitialEdgeLength = endLen;

		List<Integer> nVertices = new ArrayList<>();
		List<Long> runTimes = new ArrayList<>();
		List<Double> initlialEdgeLengths = new ArrayList<>();

		while (initialEdgeLength >= minInitialEdgeLength) {
			initlialEdgeLengths.add(initialEdgeLength);
			CLEikMeshHE meshGenerator = new CLEikMeshHE(distanceFunc, uniformEdgeLength, initialEdgeLength, bbox, new ArrayList<>(), supplier);
			meshGenerator.initialize();

			StopWatch overAllTime = new StopWatch();

			int steps = 0;
			overAllTime.start();
			overAllTime.suspend();
			do {
				overAllTime.resume();
				meshGenerator.improve();
				overAllTime.suspend();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				steps++;
			} while (!meshGenerator.isFinished());
			meshGenerator.finish();

			log.info("initial edge length: " + initialEdgeLength);
			log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
			log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
			log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
			log.info("quality: " + meshGenerator.getQuality());
			log.info("#step: " + steps);
			log.info("overall time: " + overAllTime.getTime() + "[ms]");
			log.info("step avg time: " + (double)overAllTime.getNanoTime() / steps + "[ns]");
			log.info("step avg time: " + (double)overAllTime.getTime() / steps + "[ms]");

			nVertices.add(meshGenerator.getMesh().getVertices().size());
			runTimes.add( overAllTime.getTime());

			MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800);
			JFrame frame = distmeshPanel.display();
			frame.setVisible(true);
			frame.setTitle("uniformRing()");
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			distmeshPanel.repaint();

			//initialEdgeLength = initialEdgeLength - 0.05;
			initialEdgeLength = initialEdgeLength - stepLen;
		}

		System.out.println("print result for vertex based GPU version");
		System.out.println("#vertices: [" + nVertices.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("runtime in ms: [" + runTimes.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("init edge lengths: [" + initlialEdgeLengths.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
	}

	public static void main(String[] args) throws OpenCLException {
		stepUniformRing(0.05, 0.05, 0.05);
	}
}

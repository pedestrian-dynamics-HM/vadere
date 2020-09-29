package org.vadere.meshing.mesh.triangulation.plots.qualities;

import org.apache.commons.lang3.time.StopWatch;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.improver.distmesh.DistmeshPanel;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.improver.distmesh.Distmesh;
import org.vadere.meshing.utils.io.IOUtils;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

/**
 * @author Benedikt Zoennchen
 */
public class RunTimeCPU extends JFrame {

    private static final Logger log = Logger.getLogger(RunTimeCPU.class);

	/**
	 * Each geometry is contained this bounding box.
	 */
	private static final VRectangle bbox = new VRectangle(-1.01, -1.01, 2.02, 2.02);
	private static final IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static final IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y, false);
	private static final double initialEdgeLength = 1.5;


    private static void overallUniformRing() {
	    IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();
	    IDistanceFunction distanceFunc = p -> Math.abs(0.7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 0.3;
	    List<VShape> obstacles = new ArrayList<>();

	    double initialEdgeLength = 1.5;
	    double minInitialEdgeLength = 0.03;

	    while (initialEdgeLength >= minInitialEdgeLength) {
		    GenEikMesh<AVertex, AHalfEdge, AFace> meshGenerator = new GenEikMesh<>(
				    distanceFunc,
				    uniformEdgeLength,
				    initialEdgeLength,
				    bbox, obstacles,
				    supplier);

		    StopWatch overAllTime = new StopWatch();
		    overAllTime.start();
		    meshGenerator.generate();
		    overAllTime.stop();

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

		    initialEdgeLength = initialEdgeLength * 0.5;
	    }
	}

	private static void stepAdaptiveRingEikMesh(double startLen, double endLen, double stepLen) {
		IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();
		IDistanceFunction distanceFunc = p -> Math.abs(0.7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 0.3;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = startLen;
		double minInitialEdgeLength = endLen;

		List<Integer> nVertices = new ArrayList<>();
		List<Double> qualities = new ArrayList<>();
		List<Double> minQualities = new ArrayList<>();
		List<Long> runTimes = new ArrayList<>();
		List<Double> initlialEdgeLengths = new ArrayList<>();

		int count = 1;
		while (initialEdgeLength >= minInitialEdgeLength) {
			initlialEdgeLengths.add(initialEdgeLength);
			final double currentEdgeLen = initialEdgeLength;
			IEdgeLengthFunction adaptiveEdgeLength =  p -> currentEdgeLen + Math.max(-distanceFunc.apply(p), 0) * 0.4;
			GenEikMesh<AVertex, AHalfEdge, AFace> meshGenerator = new GenEikMesh<>(
					distanceFunc,
					adaptiveEdgeLength,
					initialEdgeLength,
					bbox, obstacles,
					supplier);

			while (!meshGenerator.isInitialized()) {
				meshGenerator.initialize();
			}

			StopWatch overAllTime = new StopWatch();

			int steps = 0;
			overAllTime.start();
			overAllTime.suspend();
			do {
				overAllTime.resume();
				meshGenerator.improve();
				overAllTime.suspend();
				steps++;
			} while (!meshGenerator.isFinished());

			log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
			log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
			log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
			log.info("quality: " + meshGenerator.getQuality());
			log.info("#step: " + steps);
			log.info("overall time: " + overAllTime.getTime() + "[ms]");
			log.info("step avg time: " + overAllTime.getTime() / steps + "[ms]");

			nVertices.add(meshGenerator.getMesh().getVertices().size());
			qualities.add(meshGenerator.getQuality());
			minQualities.add(meshGenerator.getMinQuality());
			runTimes.add( overAllTime.getTime());


			try {
				File dir = new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/eikmesh/");
				BufferedWriter meshWriter = null;
				meshWriter = IOUtils.getWriter("ring_eik_"+count+".tex", dir);
				meshWriter.write(TexGraphGenerator.toTikz(meshGenerator.getMesh(), 1.0f, true));
				meshWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			count++;

			MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel<>(meshGenerator.getMesh(),1000, 800);
			JFrame frame = distmeshPanel.display();
			frame.setVisible(true);
			frame.setTitle("uniformRing()");
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			distmeshPanel.repaint();

			initialEdgeLength = initialEdgeLength * 0.5;

		}

		// 200 steps
		System.out.println("print overall runtimes for CPU");
		System.out.println("#vertices: [" + nVertices.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("runtime in ms: [" + runTimes.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("init edge lengths: [" + initlialEdgeLengths.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("qualities: [" + qualities.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("min-qualities: [" + minQualities.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
	}

	private static void stepAdaptiveRingDistMesh(double startLen, double endLen, double stepLen) {
		IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();
		IDistanceFunction distanceFunc = p -> Math.abs(0.7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 0.3;
		//IEdgeLengthFunction adaptiveEdgeLength =  p -> 1.0 + Math.max(-distanceFunc.apply(p), 0) * 0.4;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = startLen;
		double minInitialEdgeLength = endLen;

		List<Integer> nVertices = new ArrayList<>();
		List<Double> qualities = new ArrayList<>();
		List<Double> minQualities = new ArrayList<>();
		List<Long> runTimes = new ArrayList<>();
		List<Double> initlialEdgeLengths = new ArrayList<>();

		while (initialEdgeLength >= minInitialEdgeLength) {
			initlialEdgeLengths.add(initialEdgeLength);
			final double currentEdgeLen = initialEdgeLength;
			IEdgeLengthFunction adaptiveEdgeLength =  p -> currentEdgeLen + Math.max(-distanceFunc.apply(p), 0) * 0.4;
			Distmesh meshGenerator = new Distmesh(distanceFunc,
					adaptiveEdgeLength,
					initialEdgeLength,
					bbox, obstacles);

			StopWatch overAllTime = new StopWatch();

			int steps = 0;
			overAllTime.start();
			overAllTime.suspend();
			do {
				overAllTime.resume();
				meshGenerator.step();
				overAllTime.suspend();
				steps++;
			} while (steps <= 100);
			meshGenerator.reTriangulate();

			log.info("#vertices: " + meshGenerator.getPoints().size());
			log.info("quality: " + meshGenerator.getQuality());
			log.info("#step: " + steps);
			log.info("#tris: " + meshGenerator.getNumberOfReTriangulations());
			log.info("overall time: " + overAllTime.getTime() + "[ms]");
			log.info("step avg time: " + overAllTime.getTime() / steps + "[ms]\n");

			nVertices.add(meshGenerator.getPoints().size());
			runTimes.add( overAllTime.getTime());
			qualities.add(meshGenerator.getQuality());
			minQualities.add(meshGenerator.getMinQuality());

			DistmeshPanel distmeshPanel = new DistmeshPanel(meshGenerator, 1000, 800, bbox, t -> false);
			JFrame frame = distmeshPanel.display();
			frame.setVisible(true);
			frame.setTitle("uniformRing()");
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			distmeshPanel.repaint();

			initialEdgeLength = initialEdgeLength * 0.5;

		}

		// 200 steps
		System.out.println("print overall runtimes for CPU");
		System.out.println("#vertices: [" + nVertices.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("runtime in ms: [" + runTimes.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("init edge lengths: [" + initlialEdgeLengths.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("qualities: [" + qualities.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("min-qualities: [" + minQualities.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
	}

    public static void main(String[] args) {
		//stepAdaptiveRingDistMesh(0.2, 0.001, 0.001);
	    stepAdaptiveRingEikMesh(0.2, 0.02, 0.001);
	    //stepAdaptiveRingEikMesh(0.005, 0.005, 0.01);
    }
}

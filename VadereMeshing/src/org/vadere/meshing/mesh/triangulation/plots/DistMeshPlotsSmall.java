package org.vadere.meshing.mesh.triangulation.plots;

import org.apache.commons.lang3.time.StopWatch;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.distmesh.Distmesh;
import org.vadere.meshing.mesh.triangulation.improver.distmesh.DistmeshPanel;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

/**
 * Created by bzoennchen on 23.03.18.
 */
public class DistMeshPlotsSmall {
	private static final Logger log = Logger.getLogger(RunTimeCPU.class);

	/**
	 * Each geometry is contained this bounding box.
	 */
	private static final VRectangle bbox = new VRectangle(-1.01, -1.01, 2.02, 2.02);
	private static IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y, false);
	private static double initialEdgeLength = 0.06;

	/**
	 * A circle with radius 10.0 meshed using a uniform mesh.
	 */
	private static void uniformCircle(final double initialEdgeLength) throws InterruptedException {
		IDistanceFunction distanceFunc = p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 1;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p)) * 2;

		Distmesh meshGenerator = new Distmesh(distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles);

		/*StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		meshGenerator.execute();
		overAllTime.stop();

		log.info("#vertices:" + meshGenerator.getPoints().size());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");
		log.info("quality:" + meshGenerator.getQuality());
		log.info("min-quality: " + meshGenerator.getMinQuality());*/

		DistmeshPanel distmeshPanel = new DistmeshPanel(meshGenerator, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformCircle("+ initialEdgeLength +")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		while (true) {
			meshGenerator.improve();
			Thread.sleep(100);
			distmeshPanel.repaint();
			Thread.sleep(100);
		}

		//System.out.println();
		//System.out.println();
		//System.out.println(TexGraphGenerator.toTikz(meshGenerator.getMesh()));
	}

	/**
	 * A ring innter radius 4.0 and outer radius 10.0 meshed using a uniform mesh.
	 */
	private static void adaptedRing() {
		IDistanceFunction distanceFunc = p -> Math.abs(0.7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 0.3;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p)) * 2;

		Distmesh meshGenerator = new Distmesh(distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles);

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		meshGenerator.execute();
		overAllTime.stop();

		log.info("#vertices:" + meshGenerator.getPoints().size());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");
		log.info("quality:" + meshGenerator.getQuality());
		log.info("min-quality: " + meshGenerator.getMinQuality());

		DistmeshPanel distmeshPanel = new DistmeshPanel(meshGenerator, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformCircle("+ initialEdgeLength +")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		System.out.println();
		System.out.println();
	}


	/**
	 * A ring innter radius 4.0 and outer radius 10.0 meshed using a uniform mesh.
	 */
	private static void uniformRing() {
		IDistanceFunction distanceFunc = p -> Math.abs(0.7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 0.3;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = uniformEdgeLength;

		Distmesh meshGenerator = new Distmesh(distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles);

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		meshGenerator.execute();
		overAllTime.stop();

		log.info("#vertices:" + meshGenerator.getPoints().size());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");
		log.info("quality:" + meshGenerator.getQuality());
		log.info("min-quality: " + meshGenerator.getMinQuality());

		DistmeshPanel distmeshPanel = new DistmeshPanel(meshGenerator, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformCircle("+ initialEdgeLength +")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		System.out.println();
		System.out.println();
	}

	/**
	 * A circle with radius 10.0 meshed using a uniform mesh.
	 */
	private static void adaptiveRing(final double initialEdgeLength) {
		IDistanceFunction distanceFunc = p -> Math.abs(0.7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 0.3;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p)) * 2.0;
		Distmesh meshGenerator = new Distmesh(distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles);

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		meshGenerator.execute();
		overAllTime.stop();

		log.info("#vertices:" + meshGenerator.getPoints().size());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");
		log.info("quality:" + meshGenerator.getQuality());
		log.info("min-quality: " + meshGenerator.getMinQuality());

		DistmeshPanel distmeshPanel = new DistmeshPanel(meshGenerator, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformCircle("+ initialEdgeLength +")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		System.out.println();
		System.out.println();
	}

	/**
	 * A a rectangular "ring".
	 */
	private static void uniformRect() {
		VRectangle rect = new VRectangle(-0.4, -0.4, 0.8, 0.8);
		IDistanceFunction distanceFunc = IDistanceFunction.intersect(p -> Math.max(Math.abs(p.getX()), Math.abs(p.getY())) - 1.0, IDistanceFunction.create(bbox, rect));
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = uniformEdgeLength;

		obstacles.add(rect);

		Distmesh meshGenerator = new Distmesh(distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles);

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		meshGenerator.execute();
		overAllTime.stop();

		log.info("#vertices:" + meshGenerator.getPoints().size());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");
		log.info("quality:" + meshGenerator.getQuality());
		log.info("min-quality: " + meshGenerator.getMinQuality());

		DistmeshPanel distmeshPanel = new DistmeshPanel(meshGenerator, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformCircle("+ initialEdgeLength +")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		System.out.println();
		System.out.println();
	}

	/**
	 * A a rectangular "ring".
	 */
	private static void uniformHex() {
		VPolygon hex = VShape.generateHexagon(0.4);

		IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();
		IDistanceFunction distanceFunc = IDistanceFunction.intersect(p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 1.0, IDistanceFunction.create(bbox, hex));
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = uniformEdgeLength;

		obstacles.add(hex);

		Distmesh meshGenerator = new Distmesh(distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles);

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		meshGenerator.execute();
		overAllTime.stop();

		log.info("#vertices:" + meshGenerator.getPoints().size());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");
		log.info("quality:" + meshGenerator.getQuality());
		log.info("min-quality: " + meshGenerator.getMinQuality());

		DistmeshPanel distmeshPanel = new DistmeshPanel(meshGenerator, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformCircle("+ initialEdgeLength +")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		System.out.println();
		System.out.println();
	}

	public static void main(String[] args) throws InterruptedException {
		uniformCircle(initialEdgeLength);
		//uniformCircle(initialEdgeLength);
		//uniformCircle(initialEdgeLength / 2.0);
		//adaptiveRing(0.04);
		// /uniformRing();
		//uniformRect();
		//uniformHex();
		//adaptiveRing(initialEdgeLength / 2.0);
	}

}

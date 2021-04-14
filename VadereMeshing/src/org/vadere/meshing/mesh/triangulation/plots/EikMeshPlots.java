package org.vadere.meshing.mesh.triangulation.plots;

import org.apache.commons.lang3.time.StopWatch;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.*;

/**
 * This class generates some nice Meshes for different geometries / distance functions.
 *
 * @author Benedikt Zoennchen
 */
public class EikMeshPlots {

	private static final Logger log = Logger.getLogger(RunTimeCPU.class);

	/**
	 * Each geometry is contained this bounding box.
	 */
	private static final VRectangle bbox = new VRectangle(-1.01, -1.01, 2.02, 2.02);
	private static IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y, false);
	private static double initialEdgeLength = 0.1;

	/**
	 * A circle with radius 10.0 meshed using a uniform mesh.
	 */
	private static void uniformCircle(final double initialEdgeLength) {
		IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();
		IDistanceFunction distanceFunc = p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 1;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p)) * 2;

		GenEikMesh<AVertex, AHalfEdge, AFace> meshGenerator = new GenEikMesh<>(
				distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		meshGenerator.generate();
		overAllTime.stop();

		log.info("#vertices:" + meshGenerator.getMesh().getVertices().size());
		log.info("#edges:" + meshGenerator.getMesh().getEdges().size());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");
		log.info("quality:" + meshGenerator.getQuality());
		log.info("min-quality: " + meshGenerator.getMinQuality());

		Predicate<AFace> predicate = f ->  meshGenerator.faceToQuality(f) < 0.9;
		MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel(meshGenerator.getMesh(),
				predicate, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformCircle("+ initialEdgeLength +")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		System.out.println();
		System.out.println();
		//System.out.println(TexGraphGenerator.toTikz(meshGenerator.getMesh()));
	}


	/**
	 * A ring innter radius 4.0 and outer radius 10.0 meshed using a uniform mesh.
	 */
	private static void uniformRing() {
		IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();
		IDistanceFunction distanceFunc = p -> Math.abs(0.7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 0.3;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = uniformEdgeLength;

		GenEikMesh<AVertex, AHalfEdge, AFace> meshGenerator = new GenEikMesh<>(
				distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		meshGenerator.generate();
		overAllTime.stop();

		log.info("#vertices:" + meshGenerator.getMesh().getVertices().size());
		log.info("#edges:" + meshGenerator.getMesh().getEdges().size());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");
		log.info("min-quality: " + meshGenerator.getMinQuality());

		MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		System.out.println();
		System.out.println();
		System.out.println(TexGraphGenerator.toTikz(meshGenerator.getMesh()));
	}

	/**
	 * A circle with radius 10.0 meshed using a uniform mesh.
	 */
	private static void adaptiveRing(final double initialEdgeLength) {
		IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();
		IDistanceFunction distanceFunc = p -> Math.abs(0.7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 0.3;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = p -> initialEdgeLength + Math.abs(distanceFunc.apply(p));

		GenEikMesh<AVertex, AHalfEdge, AFace> meshGenerator = new GenEikMesh<>(
				distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		meshGenerator.generate();
		overAllTime.stop();

		log.info("#vertices:" + meshGenerator.getMesh().getVertices().size());
		log.info("#edges:" + meshGenerator.getMesh().getEdges().size());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");
		log.info("min-quality: " + meshGenerator.getMinQuality());

		MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("adaptiveCircle("+ initialEdgeLength + ")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		System.out.println();
		System.out.println();
		//System.out.println(TexGraphGenerator.toTikz(meshGenerator.getMesh()));
	}

	/**
	 * A a rectangular "ring".
	 */
	private static void uniformRect() {
		VRectangle rect = new VRectangle(-0.4, -0.4, 0.8, 0.8);

		IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();
		IDistanceFunction distanceFunc = IDistanceFunction.intersect(p -> Math.max(Math.abs(p.getX()), Math.abs(p.getY())) - 1.0, IDistanceFunction.create(bbox, rect));
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = uniformEdgeLength;

		obstacles.add(rect);

		GenEikMesh<AVertex, AHalfEdge, AFace> meshGenerator = new GenEikMesh<>(
				distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		/*StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		meshGenerator.generate();
		overAllTime.stop();*/

		log.info("#vertices:" + meshGenerator.getMesh().getVertices().size());
		log.info("#edges:" + meshGenerator.getMesh().getEdges().size());
		//log.info("overall time: " + overAllTime.getTime() + "[ms]");
		log.info("min-quality: " + meshGenerator.getMinQuality());

		MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRect()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		StopWatch overAllTime = new StopWatch();
		while (!meshGenerator.isFinished()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			meshGenerator.improve();
			distmeshPanel.repaint();
		}

		distmeshPanel.repaint();

		System.out.println();
		System.out.println();
		System.out.println(TexGraphGenerator.toTikz(meshGenerator.getMesh()));
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

		GenEikMesh<AVertex, AHalfEdge, AFace> meshGenerator = new GenEikMesh<>(
				distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		meshGenerator.generate();
		overAllTime.stop();

		log.info("#vertices:" + meshGenerator.getMesh().getVertices().size());
		log.info("#edges:" + meshGenerator.getMesh().getEdges().size());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");
		log.info("min-quality: " + meshGenerator.getMinQuality());

		MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformHex()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		System.out.println();
		System.out.println();
		System.out.println(TexGraphGenerator.toTikz(meshGenerator.getMesh()));
	}

	/**
	 * A a rectangular "ring".
	 */
	private static void adaptiveRect(final double initialEdgeLength) {
		VPolygon hex = VShape.generateHexagon(0.4);

		IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();
		IDistanceFunction distanceFunc = IDistanceFunction.intersect(p -> Math.max(Math.abs(p.getX()), Math.abs(p.getY()))- 1.0, IDistanceFunction.create(bbox, hex));
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunction = p -> 1.0 + Math.max(-distanceFunc.apply(p), 0) * 8.0;

		obstacles.add(hex);
		obstacles.add(new VRectangle(-1, -1, 2, 2));

		GenEikMesh<AVertex, AHalfEdge, AFace> meshGenerator = new GenEikMesh<>(
				distanceFunc,
				edgeLengthFunction,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		meshGenerator.generate();
		overAllTime.stop();

		log.info("#vertices:" + meshGenerator.getMesh().getVertices().size());
		log.info("#edges:" + meshGenerator.getMesh().getEdges().size());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");
		log.info("min-quality: " + meshGenerator.getMinQuality());

		MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformHex()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		System.out.println();
		System.out.println();
		System.out.println(TexGraphGenerator.toTikz(meshGenerator.getMesh()));
	}

	private EikMeshPlots() {

	}

	public static void main(String[] args) {
		adaptiveRect(0.05);
		adaptiveRing(0.2);
		uniformCircle(initialEdgeLength);
		uniformCircle(initialEdgeLength / 2.0);
		uniformRing();
		uniformRect();
		uniformHex();
		adaptiveRing(initialEdgeLength / 2.0);
	}


}

package org.vadere.util.triangulation.adaptive.plots;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AHalfEdge;
import org.vadere.util.geometry.mesh.gen.AMesh;
import org.vadere.util.geometry.mesh.gen.AVertex;
import org.vadere.util.geometry.mesh.inter.IMeshSupplier;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.triangulation.IPointConstructor;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;
import org.vadere.util.triangulation.adaptive.MeshPoint;
import org.vadere.util.triangulation.adaptive.PSMeshingPanel;
import org.vadere.util.triangulation.improver.PSMeshing;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

/**
 * This class generates some nice Meshes for different geometries / distance functions.
 *
 * @author Benedikt Zoennchen
 */
public class MeshPlots {

	private static final Logger log = LogManager.getLogger(RunTimeCPU.class);

	/**
	 * Each geometry is contained this bounding box.
	 */
	private static final VRectangle bbox = new VRectangle(-11, -11, 22, 22);
	private static IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static IPointConstructor<MeshPoint> pointConstructor = (x, y) -> new MeshPoint(x, y, false);
	private static double initialEdgeLength = 1.5;

	/**
	 * A circle with radius 10.0 meshed using a uniform mesh.
	 */
	private static void uniformCircle(final double initialEdgeLength) {
		IMeshSupplier<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 10;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = uniformEdgeLength;

		PSMeshing<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> meshGenerator = new PSMeshing<>(
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

		PSMeshingPanel<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> distmeshPanel = new PSMeshingPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformCircle("+ initialEdgeLength +")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();
	}


	/**
	 * A ring innter radius 4.0 and outer radius 10.0 meshed using a uniform mesh.
	 */
	private static void uniformRing() {
		IMeshSupplier<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = uniformEdgeLength;

		PSMeshing<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> meshGenerator = new PSMeshing<>(
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

		PSMeshingPanel<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> distmeshPanel = new PSMeshingPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();
	}

	/**
	 * A circle with radius 10.0 meshed using a uniform mesh.
	 */
	private static void adaptiveRing(final double initialEdgeLength) {
		IMeshSupplier<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = p -> initialEdgeLength + Math.abs(distanceFunc.apply(p)) * 0.5;

		PSMeshing<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> meshGenerator = new PSMeshing<>(
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

		PSMeshingPanel<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> distmeshPanel = new PSMeshingPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("adaptiveCircle("+ initialEdgeLength + ")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();
	}

	/**
	 * A a rectangular "ring".
	 */
	private static void uniformRect() {
		VRectangle rect = new VRectangle(-4, -4, 8, 8);

		IMeshSupplier<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = IDistanceFunction.intersect(p -> Math.max(Math.abs(p.getX()), Math.abs(p.getY())) - 10, IDistanceFunction.create(bbox, rect));
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = uniformEdgeLength;

		obstacles.add(rect);

		PSMeshing<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> meshGenerator = new PSMeshing<>(
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

		PSMeshingPanel<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> distmeshPanel = new PSMeshingPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRect()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();
	}

	/**
	 * A a rectangular "ring".
	 */
	private static void uniformHex() {
		VPolygon hex = VShape.generateHexagon(4.0);

		IMeshSupplier<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = IDistanceFunction.intersect(p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 10, IDistanceFunction.create(bbox, hex));
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = uniformEdgeLength;

		obstacles.add(hex);

		PSMeshing<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> meshGenerator = new PSMeshing<>(
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

		PSMeshingPanel<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> distmeshPanel = new PSMeshingPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformHex()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();
	}

	private MeshPlots() {

	}

	public static void main(String[] args) {
		uniformCircle(initialEdgeLength * 2.0);
		uniformCircle(initialEdgeLength);
		uniformCircle(initialEdgeLength / 2.0);
		uniformRing();
		uniformRect();
		uniformHex();
		adaptiveRing(initialEdgeLength / 2.0);
	}


}

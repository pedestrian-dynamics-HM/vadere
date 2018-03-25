package org.vadere.util.triangulation.adaptive.plots.qualities;

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
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.triangulation.IPointConstructor;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;
import org.vadere.util.triangulation.adaptive.MeshPoint;
import org.vadere.util.triangulation.adaptive.PSDistmesh;
import org.vadere.util.triangulation.adaptive.PSDistmeshPanel;
import org.vadere.util.triangulation.adaptive.PSMeshingPanel;
import org.vadere.util.triangulation.improver.PSMeshing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.*;

public class TriangleQuality {
	private static final Logger log = LogManager.getLogger(TriangleQuality.class);

	/**
	 * Each geometry is contained this bounding box.
	 */
	private static final VRectangle bbox = new VRectangle(-1.01, -1.01, 2.02, 2.02);
	private static final IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static final IPointConstructor<MeshPoint> pointConstructor = (x, y) -> new MeshPoint(x, y, false);
	private static final double initialEdgeLength = 1.5;


	private static void adaptiveDistMesh(double startLen) {
		IDistanceFunction distanceFunc = p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 1.0;
		IEdgeLengthFunction edgeLengthFunction = p -> 1.0 + Math.max(-distanceFunc.apply(p), 0) * 2.0;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = startLen;

		List<Integer> steps = new ArrayList<>();
		List<Double> quality = new ArrayList<>();
		List<Double> minQuality = new ArrayList<>();

		PSDistmesh meshGenerator = new PSDistmesh(distanceFunc,
				edgeLengthFunction,
				initialEdgeLength,
				bbox, obstacles);

		//79 480
		StopWatch overAllTime = new StopWatch();

		overAllTime.start();
		overAllTime.suspend();
		int step = 0;
		do {
			steps.add(step);
			overAllTime.resume();
			meshGenerator.step();
			overAllTime.suspend();
			quality.add(meshGenerator.getQuality());
			minQuality.add(meshGenerator.getMinQuality());
			step++;
		} while (!meshGenerator.hasMaximalSteps());

		log.info("#vertices: " + meshGenerator.getPoints().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + steps);
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		Predicate<VTriangle> predicate = t -> meshGenerator.getQuality(t) < 0.4;
		PSDistmeshPanel distmeshPanel = new PSDistmeshPanel(meshGenerator, 1000, 800, bbox, predicate);

		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("DistMesh: adaptiveDisc("+ initialEdgeLength +")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		// 200 steps
		System.out.println("print qualities for unified tri " + initialEdgeLength);
		System.out.println("#steps: [" + steps.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("quality = [" + quality.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("minQuality = [" + minQuality.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
	}

	private static void adaptiveDiscEikMesh(double startLen) {
		IMeshSupplier<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 1.0;
		IEdgeLengthFunction edgeLengthFunction = p -> 1.0 + Math.max(-distanceFunc.apply(p), 0) * 2.0;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = startLen;

		List<Integer> steps = new ArrayList<>();
		List<Double> quality = new ArrayList<>();
		List<Double> minQuality = new ArrayList<>();

		PSMeshing<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> meshGenerator = new PSMeshing<>(
				distanceFunc,
				edgeLengthFunction,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		//79 480
		StopWatch overAllTime = new StopWatch();

		overAllTime.start();
		overAllTime.suspend();
		int step = 0;
		do {
			steps.add(step);
			overAllTime.resume();
			meshGenerator.improve();
			overAllTime.suspend();
			quality.add(meshGenerator.getQuality());
			minQuality.add(meshGenerator.getMinQuality());
			step++;
		} while (!meshGenerator.isFinished());

		log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
		log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
		log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + steps);
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		PSMeshingPanel<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> distmeshPanel = new PSMeshingPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		// 200 steps
		System.out.println("print qualities for unified tri " + initialEdgeLength);
		System.out.println("#steps: [" + steps.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("quality = [" + quality.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("minQuality = [" + minQuality.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
	}


	private static void adaptiveRingDistMesh(double startLen) {
		IDistanceFunction distanceFunc = p -> Math.abs(0.7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 0.3;
		IEdgeLengthFunction edgeLengthFunction = p -> 1.0 + Math.max(-distanceFunc.apply(p), 0) * 2.0;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = startLen;

		List<Integer> steps = new ArrayList<>();
		List<Double> quality = new ArrayList<>();
		List<Double> minQuality = new ArrayList<>();

		PSDistmesh meshGenerator = new PSDistmesh(distanceFunc,
				edgeLengthFunction,
				initialEdgeLength,
				bbox, obstacles);

		//79 480
		StopWatch overAllTime = new StopWatch();

		overAllTime.start();
		overAllTime.suspend();
		int step = 0;
		do {
			steps.add(step);
			overAllTime.resume();
			meshGenerator.step();
			overAllTime.suspend();
			quality.add(meshGenerator.getQuality());
			minQuality.add(meshGenerator.getMinQuality());
			step++;
		} while (!meshGenerator.hasMaximalSteps());

		log.info("#vertices: " + meshGenerator.getPoints().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + steps);
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		Predicate<VTriangle> predicate = t -> meshGenerator.getQuality(t) < 0.4;
		PSDistmeshPanel distmeshPanel = new PSDistmeshPanel(meshGenerator, 1000, 800, bbox, predicate);

		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("DistMesh: adaptiveDisc("+ initialEdgeLength +")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		// 200 steps
		System.out.println("print qualities for unified tri " + initialEdgeLength);
		System.out.println("#steps: [" + steps.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("quality = [" + quality.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("minQuality = [" + minQuality.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
	}


	private static void adaptiveRingEikMesh(double startLen) {
		IMeshSupplier<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = p -> Math.abs(0.7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 0.3;
		IEdgeLengthFunction edgeLengthFunction = p -> 1.0 + Math.max(-distanceFunc.apply(p), 0) * 2.0;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = startLen;

		List<Integer> steps = new ArrayList<>();
		List<Double> quality = new ArrayList<>();
		List<Double> minQuality = new ArrayList<>();

		PSMeshing<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> meshGenerator = new PSMeshing<>(
				distanceFunc,
				edgeLengthFunction,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		//79 480
		StopWatch overAllTime = new StopWatch();

		overAllTime.start();
		overAllTime.suspend();
		int step = 0;
		do {
			steps.add(step);
			overAllTime.resume();
			meshGenerator.improve();
			overAllTime.suspend();
			quality.add(meshGenerator.getQuality());
			minQuality.add(meshGenerator.getMinQuality());
			step++;
		} while (!meshGenerator.isFinished());

		log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
		log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
		log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + steps);
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		PSMeshingPanel<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> distmeshPanel = new PSMeshingPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		// 200 steps
		System.out.println("print qualities for unified tri " + initialEdgeLength);
		System.out.println("#steps: [" + steps.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("quality = [" + quality.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("minQuality = [" + minQuality.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
	}

	private static void adaptiveHexDistMesh(double startLen) {
		VPolygon hex = VShape.generateHexagon(0.4);
		IDistanceFunction quader = p -> Math.max(Math.abs(p.getX()), Math.abs(p.getY())) - 1.0;
		IDistanceFunction circ = p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 1.0;
		IDistanceFunction distanceFunc = IDistanceFunction.intersect(quader, IDistanceFunction.create(bbox, hex));


		IEdgeLengthFunction edgeLengthFunction = p -> 1.0 + Math.max(-distanceFunc.apply(p), 0) * 8.0;
		List<VShape> obstacles = new ArrayList<>();
		obstacles.add(hex);
		obstacles.add(new VRectangle(-1,-1,2,2));

		double initialEdgeLength = startLen;

		List<Integer> steps = new ArrayList<>();
		List<Double> quality = new ArrayList<>();
		List<Double> minQuality = new ArrayList<>();

		PSDistmesh meshGenerator = new PSDistmesh(distanceFunc,
				edgeLengthFunction,
				initialEdgeLength,
				bbox, obstacles);

		//79 480
		StopWatch overAllTime = new StopWatch();

		overAllTime.start();
		overAllTime.suspend();
		int step = 0;
		do {
			steps.add(step);
			overAllTime.resume();
			meshGenerator.step();
			overAllTime.suspend();
			quality.add(meshGenerator.getQuality());
			minQuality.add(meshGenerator.getMinQuality());
			step++;
		} while (!meshGenerator.hasMaximalSteps());

		log.info("#vertices: " + meshGenerator.getPoints().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + steps);
		log.info("#tris: " + meshGenerator.getNumberOfReTriangulations());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		Predicate<VTriangle> predicate = t -> meshGenerator.getQuality(t) < 0.4;
		PSDistmeshPanel distmeshPanel = new PSDistmeshPanel(meshGenerator, 1000, 800, bbox, predicate);

		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("DistMesh: adaptiveDisc("+ initialEdgeLength +")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		// 200 steps
		System.out.println("print qualities for unified tri " + initialEdgeLength);
		System.out.println("#steps: [" + steps.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("quality = [" + quality.stream().map(n -> (n-0.01)+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("minQuality = [" + minQuality.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
	}

	private static void adaptiveHexEikMesh(double startLen) {
		VPolygon hex = VShape.generateHexagon(0.4);
		IMeshSupplier<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction quader = p -> Math.max(Math.abs(p.getX()), Math.abs(p.getY())) - 1.0;
		IDistanceFunction circ = p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 1.0;
		IDistanceFunction distanceFunc = IDistanceFunction.intersect(quader, IDistanceFunction.create(bbox, hex));


		IEdgeLengthFunction edgeLengthFunction = p -> 1.0 + Math.max(-distanceFunc.apply(p), 0) * 8.0;
		List<VShape> obstacles = new ArrayList<>();
		obstacles.add(hex);
		obstacles.add(new VRectangle(-1,-1,2,2));

		double initialEdgeLength = startLen;

		List<Integer> steps = new ArrayList<>();
		List<Double> quality = new ArrayList<>();
		List<Double> minQuality = new ArrayList<>();

		PSMeshing<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> meshGenerator = new PSMeshing<>(
				distanceFunc,
				edgeLengthFunction,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		//79 480
		StopWatch overAllTime = new StopWatch();

		overAllTime.start();
		overAllTime.suspend();
		int step = 0;
		do {
			steps.add(step);
			overAllTime.resume();
			meshGenerator.improve();
			overAllTime.suspend();
			quality.add(meshGenerator.getQuality());
			minQuality.add(meshGenerator.getMinQuality());
			step++;
		} while (!meshGenerator.isFinished());

		log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
		log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
		log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + steps);
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		PSMeshingPanel<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> distmeshPanel = new PSMeshingPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		// 200 steps
		System.out.println("print qualities for unified tri " + initialEdgeLength);
		System.out.println("#steps: [" + steps.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("quality = [" + quality.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("minQuality = [" + minQuality.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
	}


	public static void main(String[] args) {
		//adaptiveDiscEikMesh(0.1);
		//adaptiveDistMesh(0.06);

		//adaptiveDiscEikMesh(0.05);
		//adaptiveDistMesh(0.03);

		adaptiveHexEikMesh(0.05);
		adaptiveHexDistMesh(0.03);
	}
}

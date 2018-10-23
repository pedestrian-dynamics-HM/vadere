package org.vadere.meshing.mesh.triangulation.plots;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.inter.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPanel;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMesh;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

public class  TriangleQuality{
	private static final Logger log = LogManager.getLogger(TriangleQuality.class);

	/**
	 * Each geometry is contained this bounding box.
	 */
	private static final VRectangle bbox = new VRectangle(-11, -11, 22, 22);
	private static final IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static final IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y, false);
	private static final double initialEdgeLength = 1.5;

	private static void stepUniformRing(double startLen) {
		IMeshSupplier<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = startLen;

		List<Integer> steps = new ArrayList<>();
		List<Double> quality = new ArrayList<>();

		EikMesh<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> meshGenerator = new EikMesh<>(
				distanceFunc,
				uniformEdgeLength,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		//79 480
		StopWatch overAllTime = new StopWatch();

		overAllTime.start();
		overAllTime.suspend();
		int step = 0;
		do {
			quality.add(meshGenerator.getQuality());
			steps.add(step);
			overAllTime.resume();
			meshGenerator.improve();
			overAllTime.suspend();
			step++;
		} while (!meshGenerator.isFinished());

		log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
		log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
		log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + steps);
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		EikMeshPanel<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> distmeshPanel = new EikMeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		// 200 steps
		System.out.println("print qualities for unified tri " + initialEdgeLength);
		System.out.println("#steps: [" + steps.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("quality: [" + quality.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
	}

	private static void resultUniformRing(double startLen) {
		IMeshSupplier<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = startLen;
		List<Double> quality = new ArrayList<>();

		EikMesh<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> meshGenerator = new EikMesh<>(
				distanceFunc,
				uniformEdgeLength,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		//79 480
		StopWatch overAllTime = new StopWatch();

		overAllTime.start();
		overAllTime.suspend();
		int step = 0;
		do {
			overAllTime.resume();
			meshGenerator.improve();
			overAllTime.suspend();
			step++;
		} while (!meshGenerator.isFinished());

		log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
		log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
		log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + step);
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		EikMeshPanel<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> distmeshPanel = new EikMeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();


		System.out.println("print qualities for unified tri " + initialEdgeLength);
		System.out.println("quality: [" + meshGenerator.getMesh().getFaces().stream().map(f -> meshGenerator.faceToQuality(f)).map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("#none acute triangles: " + meshGenerator.getMesh().getFaces().stream().map(f -> meshGenerator.getMesh().toTriangle(f)).filter(t -> t.isNonAcute()).count());
	}

	private static void resultAdaptiveRing(double startLen) {
		IMeshSupplier<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = p -> initialEdgeLength + Math.abs(distanceFunc.apply(p)) * 0.5;

		double initialEdgeLength = startLen;
		List<Double> quality = new ArrayList<>();

		EikMesh<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> meshGenerator = new EikMesh<>(
				distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		//79 480
		StopWatch overAllTime = new StopWatch();

		overAllTime.start();
		overAllTime.suspend();
		int step = 0;
		do {
			overAllTime.resume();
			meshGenerator.improve();
			overAllTime.suspend();
			step++;
		} while (!meshGenerator.isFinished());

		log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
		log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
		log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + step);
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		EikMeshPanel<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> distmeshPanel = new EikMeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();


		System.out.println("print qualities for unified tri " + initialEdgeLength);
		System.out.println("quality: [" + meshGenerator.getMesh().getFaces().stream().map(f -> meshGenerator.faceToQuality(f)).map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("#none acute triangles: " + meshGenerator.getMesh().getFaces().stream().map(f -> meshGenerator.getMesh().toTriangle(f)).filter(t -> t.isNonAcute()).count());
	}

	private static void stepAdativeRing(double startLen) {
		IMeshSupplier<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> supplier = () -> new AMesh<>(pointConstructor);
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = startLen;
		IEdgeLengthFunction edgeLengthFunc = p -> initialEdgeLength + Math.abs(distanceFunc.apply(p)) * 0.5;

		List<Integer> steps = new ArrayList<>();
		List<Double> quality = new ArrayList<>();

		EikMesh<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> meshGenerator = new EikMesh<>(
				distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		//79 480
		StopWatch overAllTime = new StopWatch();

		overAllTime.start();
		overAllTime.suspend();
		int step = 0;
		do {
			quality.add(meshGenerator.getQuality());
			steps.add(step);
			overAllTime.resume();
			meshGenerator.improve();
			overAllTime.suspend();
			step++;
		} while (!meshGenerator.isFinished());

		log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
		log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
		log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + steps);
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		EikMeshPanel<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> distmeshPanel = new EikMeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800, bbox);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		// 200 steps
		System.out.println("print qualities for unified tri " + initialEdgeLength);
		System.out.println("#steps: [" + steps.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("quality: [" + quality.stream().map(n -> n+"").reduce("", (s1,s2) -> s1 + "," + s2).substring(1) + "]");
	}

	public static void main(String[] args) {
		resultAdaptiveRing(0.2);
	}
}

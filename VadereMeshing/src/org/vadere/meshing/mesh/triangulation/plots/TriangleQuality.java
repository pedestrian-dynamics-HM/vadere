package org.vadere.meshing.mesh.triangulation.plots;

import org.apache.commons.lang3.time.StopWatch;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.*;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AFace;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AVertex;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles.ATriangleMesh;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles.ATriangleMeshBuilder;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

public class  TriangleQuality{
	private static final Logger log = Logger.getLogger(TriangleQuality.class);

	/**
	 * Each geometry is contained this bounding box.
	 */
	private static final VRectangle bbox = new VRectangle(-11, -11, 22, 22);
	private static final IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static final IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y, false);
	private static final double initialEdgeLength = 1.5;

	private static void stepUniformRing(double startLen) {
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = startLen;

		List<Integer> steps = new ArrayList<>();
		List<Double> quality = new ArrayList<>();

		GenEikMesh<AVertex, AHalfEdge, AFace> meshGenerator = new GenEikMesh<>(
				distanceFunc,
				uniformEdgeLength,
				initialEdgeLength,
				bbox, obstacles,
				ATriangleMeshBuilder::new);

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

		log.info("#vertices: " + meshGenerator.getMeshBuilder().getMesh().vertices().count());
		log.info("#edges: " + meshGenerator.getMeshBuilder().getMesh().edges().count());
		log.info("#faces: " + meshGenerator.getMeshBuilder().getMesh().faces().count());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + steps);
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800);
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
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = startLen;
		List<Double> quality = new ArrayList<>();

		GenEikMesh<AVertex, AHalfEdge, AFace> meshGenerator = new GenEikMesh<>(
				distanceFunc,
				uniformEdgeLength,
				initialEdgeLength,
				bbox, obstacles,
				ATriangleMeshBuilder::new);

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

		log.info("#vertices: " + meshGenerator.getMeshBuilder().getMesh().vertices().count());
		log.info("#edges: " + meshGenerator.getMeshBuilder().getMesh().edges().count());
		log.info("#faces: " + meshGenerator.getMeshBuilder().getMesh().faces().count());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + step);
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();


		System.out.println("print qualities for unified tri " + initialEdgeLength);
		System.out.println("quality: [" + meshGenerator.getMesh().faces().stream().map(f -> meshGenerator.faceToQuality(f)).map(n -> n+"").reduce("", (s1, s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("#none acute triangles: " + meshGenerator.getMesh().faces().stream().map(f -> meshGenerator.getMeshBuilder().getMesh().faces().toTriangle(f)).filter(t -> t.isNonAcute()).count());
	}

	private static void resultAdaptiveRing(double startLen) {
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();
		IEdgeLengthFunction edgeLengthFunc = p -> initialEdgeLength + Math.abs(distanceFunc.apply(p)) * 0.5;

		double initialEdgeLength = startLen;
		List<Double> quality = new ArrayList<>();

		GenEikMesh<AVertex, AHalfEdge, AFace> meshGenerator = new GenEikMesh<>(
				distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles,
				ATriangleMeshBuilder::new);

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

		log.info("#vertices: " + meshGenerator.getMeshBuilder().getMesh().vertices().count());
		log.info("#edges: " + meshGenerator.getMeshBuilder().getMesh().edges().count());
		log.info("#faces: " + meshGenerator.getMeshBuilder().getMesh().faces().count());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + step);
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel<>(meshGenerator.getMesh(), f -> false, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();


		System.out.println("print qualities for unified tri " + initialEdgeLength);
		System.out.println("quality: [" + meshGenerator.getMeshBuilder().getMesh().faces().stream().map(f -> meshGenerator.faceToQuality(f)).map(n -> n+"").reduce("", (s1, s2) -> s1 + "," + s2).substring(1) + "]");
		System.out.println("#none acute triangles: " + meshGenerator.getMeshBuilder().getMesh().faces().stream().map(f -> meshGenerator.getMeshBuilder().getMesh().faces().toTriangle(f)).filter(t -> t.isNonAcute()).count());
	}

	private static void stepAdativeRing(double startLen) {
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
		List<VShape> obstacles = new ArrayList<>();

		double initialEdgeLength = startLen;
		IEdgeLengthFunction edgeLengthFunc = p -> initialEdgeLength + Math.abs(distanceFunc.apply(p)) * 0.5;

		List<Integer> steps = new ArrayList<>();
		List<Double> quality = new ArrayList<>();

		GenEikMesh<AVertex, AHalfEdge, AFace> meshGenerator = new GenEikMesh<>(
				distanceFunc,
				edgeLengthFunc,
				initialEdgeLength,
				bbox, obstacles,
				ATriangleMeshBuilder::new);

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

		log.info("#vertices: " + meshGenerator.getMeshBuilder().getMesh().vertices().count());
		log.info("#edges: " + meshGenerator.getMeshBuilder().getMesh().edges().count());
		log.info("#faces: " + meshGenerator.getMeshBuilder().getMesh().faces().count());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("#step: " + steps);
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

		MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel(meshGenerator.getMesh(), f -> false, 1000, 800);
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

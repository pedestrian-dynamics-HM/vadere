package org.vadere.meshing.mesh.triangulation.plots;

import org.apache.commons.lang3.time.StopWatch;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.visualization.ColorHelper;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.swing.*;

public class VisualTestCPU {

	private static final Logger log = Logger.getLogger(RunTimeGPUEdgeBased.class);

	private static final VRectangle bbox = new VRectangle(-11, -11, 22, 22);
	private static final IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static final IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y, false);
	private static final double initialEdgeLength = 0.7;

	private static void overallUniformRingA() {
		VPolygon hex = VShape.generateHexagon(4.0);
		IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;

		//IDistanceFunction distanceFunc = IDistanceFunction.intersect(p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 10, IDistanceFunction.create(bbox, hex));
		List<VShape> obstacles = new ArrayList<>();

		GenEikMesh meshGenerator = new GenEikMesh(distanceFunc, p -> 1.0 + (distanceFunc.apply(p) * distanceFunc.apply(p) / 6.0), initialEdgeLength, bbox, new ArrayList<>(), supplier);

		ColorHelper colorHelper = new ColorHelper(meshGenerator.getMesh().getNumberOfFaces());
		Function<AFace, Color> colorFunction = f -> colorHelper.numberToColor(f.getId());

		MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel<>(meshGenerator.getMesh(), f -> false, 1000, 800, colorFunction);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		//meshGenerator.improve();
		//meshGenerator.improve();
		//meshGenerator.improve();


		int nSteps = 0;
		while (nSteps < 300) {
			nSteps++;
			meshGenerator.improve();
			overAllTime.suspend();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			distmeshPanel.repaint();
			log.info("quality: " + meshGenerator.getQuality());
			log.info("min-quality: " + meshGenerator.getMinQuality());
			overAllTime.resume();
		}
		overAllTime.stop();

		log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
		log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
		log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("min-quality: " + meshGenerator.getMinQuality());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");



		System.out.println(TexGraphGenerator.toTikz(meshGenerator.getMesh(), colorFunction, 1.0f));

	}

	private static void overallUniformRingP() {
		VPolygon hex = VShape.generateHexagon(4.0);
		IMeshSupplier<PVertex, PHalfEdge, PFace> supplier = () -> new PMesh();
		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;

		//IDistanceFunction distanceFunc = IDistanceFunction.intersect(p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 10, IDistanceFunction.create(bbox, hex));
		List<VShape> obstacles = new ArrayList<>();

		GenEikMesh meshGenerator = new GenEikMesh(distanceFunc, p -> 1.0 + (distanceFunc.apply(p) * distanceFunc.apply(p) / 6.0), initialEdgeLength, bbox, new ArrayList<>(), supplier);
		meshGenerator.initialize();
		MeshPanel<PVertex, PHalfEdge, PFace> distmeshPanel = new MeshPanel<>(meshGenerator.getMesh(), f -> false, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		int nSteps = 0;

		while (nSteps < 300) {
			nSteps++;

			overAllTime.suspend();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			meshGenerator.improve();
			distmeshPanel.repaint();
			log.info("quality: " + meshGenerator.getQuality());
			log.info("min-quality: " + meshGenerator.getMinQuality());
			overAllTime.resume();
		}
		overAllTime.stop();

		log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
		log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
		log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("min-quality: " + meshGenerator.getMinQuality());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

	}

	public static void main(String[] args) {
		overallUniformRingA();
	}

}

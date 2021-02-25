package org.vadere.meshing.mesh.triangulation.plots;

import org.apache.commons.lang3.time.StopWatch;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.*;

/**
 * Created by bzoennchen on 25.03.18.
 */
public class EikMeshTests {

	private static final Logger log = Logger.getLogger(EikMeshTests.class);

	/**
	 * Each geometry is contained this bounding box.
	 */
	private static final VRectangle bbox = new VRectangle(-1.01, -1.01, 2.02, 2.02);
	private static IEdgeLengthFunction uniformEdgeLength = p -> 1.0;
	private static IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y, false);
	private static double initialEdgeLength = 0.05;


	private static void testVisual(){
		VPolygon hex = VShape.generateHexagon(0.4);
		IMeshSupplier<AVertex, AHalfEdge, AFace> supplier = () -> new AMesh();

		IDistanceFunction quader = p -> Math.max(Math.abs(p.getX()), Math.abs(p.getY())) - 1.0;
		IDistanceFunction circ = p -> Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY()) - 1.0;
		IDistanceFunction distanceFunc = IDistanceFunction.intersect(quader, IDistanceFunction.create(bbox, hex));


		IEdgeLengthFunction edgeLengthFunction = p -> 1.0 + Math.max(-distanceFunc.apply(p), 0) * 8.0;
		List<VShape> obstacles = new ArrayList<>();
		obstacles.add(hex);
		obstacles.add(new VRectangle(-1,-1,2,2));

		GenEikMesh<AVertex, AHalfEdge, AFace> meshGenerator = new GenEikMesh<>(
				distanceFunc,
				edgeLengthFunction,
				initialEdgeLength,
				bbox, obstacles,
				supplier);

		Predicate<AFace> predicate = f ->  meshGenerator.faceToQuality(f) < 0.8;
		MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel(meshGenerator.getMesh(),
				predicate, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("EikMesh: uniformCircle("+ initialEdgeLength +")");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		log.debug("#vertices: " + meshGenerator.getMesh().getPoints().size());
		int step = 0;
		while (step < 300) {

			step++;
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(step == 16) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			meshGenerator.improve();
			distmeshPanel.repaint();
			double quality = meshGenerator.getQuality();
			log.debug("quality: " + quality);
			log.debug("min-quality: " + meshGenerator.getMinQuality());
			//log.debug("min-quality: " + meshGenerator.getMinQuality());
			log.debug("step: " + step);
		}
		overAllTime.stop();

		log.info("#vertices:" + meshGenerator.getMesh().getVertices().size());
		log.info("#edges:" + meshGenerator.getMesh().getEdges().size());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");
		log.info("quality:" + meshGenerator.getQuality());
		log.info("min-quality: " + meshGenerator.getMinQuality());


		distmeshPanel.repaint();

		System.out.println();
		System.out.println();
		//System.out.println(TexGraphGenerator.toTikz(meshGenerator.getMesh()));
	}

	public static void main(String[] args) {
		testVisual();
	}
}

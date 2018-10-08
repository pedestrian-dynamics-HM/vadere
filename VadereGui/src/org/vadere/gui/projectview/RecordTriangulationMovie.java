package org.vadere.gui.projectview;

import org.apache.commons.lang3.time.StopWatch;
import org.vadere.gui.components.utils.Recorder;
import org.vadere.util.color.ColorHelper;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AHalfEdge;
import org.vadere.util.geometry.mesh.gen.AVertex;
import org.vadere.util.geometry.shapes.VDisc;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.mesh.triangulation.adaptive.DistanceFunction;
import org.vadere.util.geometry.mesh.triangulation.improver.EikMeshPoint;
import org.vadere.util.geometry.mesh.triangulation.improver.EikMeshPanel;
import org.vadere.util.geometry.mesh.triangulation.improver.AEikMesh;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.swing.*;

public class RecordTriangulationMovie {

	public static void main(String... args) throws IOException {
		VPolygon boundary = GeometryUtils.polygonFromPoints2D(
				new VPoint(0,0),
				new VPoint(0, 1),
				new VPoint(1, 2),
				new VPoint(2,1),
				new VPoint(2,0));

		VRectangle rect = new VRectangle(0.5, 0.5, 0.5, 0.5);
		final VDisc disc = new VDisc(new VPoint(1,1), 0.5);
		VRectangle bbound = new VRectangle(boundary.getBounds2D());
		List<VShape> obstacleShapes = new ArrayList<>();
		DistanceFunction distanceFunction = new DistanceFunction(boundary, Arrays.asList(disc));
		//obstacleShapes.add(rect);

		AEikMesh meshImprover = new AEikMesh(
				distanceFunction,
				p -> 1.0 + 10*Math.max(0, -distanceFunction.apply(p)),
				0.05,
				bbound,
				obstacleShapes);

		Function<AFace<EikMeshPoint>, Color> colorFunction1 = f -> {
			float q = Math.max(0.0f, Math.min(1.0f, (float) meshImprover.faceToQuality(f)));
			return new Color(q, q, q);
		};

		Function<AFace<EikMeshPoint>, Color> colorFunction2 = f -> {
			return ColorHelper.numberToHurColor((float)f.getId() / meshImprover.getMesh().getNumberOfFaces());
		};
		//ColorHelper.numberToHurColor((float)f.getId() / meshImprover.getMesh().getNumberOfFaces());
		//new ColorHelper(meshImprover.getMesh().getNumberOfFaces()).numberToColor(f.getId());

		EikMeshPanel<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> distmeshPanel = new EikMeshPanel<>(
		meshImprover.getMesh(), f -> false, bbound.getWidth()*1000, bbound.getHeight()*1000, bbound, colorFunction1);

		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("uniformRing()");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();

		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		overAllTime.suspend();
		//meshGenerator.improve();
		//meshGenerator.improve();
		//meshGenerator.improve();

		Recorder recorder = new Recorder();
		recorder.startRecording();
		boolean finished = true;
		int nSteps = 0;
		while (nSteps < 300) {
			nSteps++;

			distmeshPanel.refresh();
			if(!meshImprover.initializationFinished()) {
				addPictures(recorder, distmeshPanel, 10);
			}
			else if(finished) {
				finished = false;
				addPictures(recorder, distmeshPanel, 20);
			}

			addPictures(recorder, distmeshPanel, 5);


			/*try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
			overAllTime.resume();
			meshImprover.step();
			overAllTime.suspend();
			distmeshPanel.repaint();
		}
		meshImprover.improve();
		overAllTime.resume();
		overAllTime.stop();
		distmeshPanel.repaint();
		recorder.stopRecording();
	}

	public static void addPictures(Recorder recorder,
	                               EikMeshPanel<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> distmeshPanel,
	                               int frames) throws IOException {

		for(int i = 0; i < frames; i++) {
			recorder.addPicture(distmeshPanel.getImage());
		}

	}

}

package org.vadere.gui.projectview;

import org.apache.commons.lang3.time.StopWatch;
import org.vadere.gui.components.utils.Recorder;
import org.vadere.meshing.mesh.gen.MeshRenderer;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.AEikMesh;
import org.vadere.util.logging.StdOutErrLog;
import org.vadere.util.visualization.ColorHelper;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.util.geometry.shapes.VDisc;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.DistanceFunction;
import org.vadere.meshing.mesh.gen.MeshPanel;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.swing.*;

public class RecordTriangulationMovie {

	public static void main(String... args) throws IOException {
		StdOutErrLog.addStdOutErrToLog();
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

		Function<AFace, Color> colorFunction1 = f -> {
			float q = Math.max(0.0f, Math.min(1.0f, (float) meshImprover.faceToQuality(f)));
			return new Color(q, q, q);
		};

		Function<AFace, Color> colorFunction2 = f -> {
			return ColorHelper.numberToHurColor((float)f.getId() / meshImprover.getMesh().getNumberOfFaces());
		};
		//ColorHelper.numberToHurColor((float)f.getId() / meshImprover.getMesh().getNumberOfFaces());
		//new ColorHelper(meshImprover.getMesh().getNumberOfFaces()).numberToColor(f.getId());

		MeshRenderer<AVertex, AHalfEdge, AFace> meshRenderer = new MeshRenderer<>(
				meshImprover.getMesh(), f -> false, colorFunction1);

		MeshPanel<AVertex, AHalfEdge, AFace> distmeshPanel = new MeshPanel<>(
				meshRenderer, bbound.getWidth()*1000, bbound.getHeight()*1000);

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

			if(!meshImprover.isInitialized()) {
				addPictures(recorder, meshRenderer, 10, (int)bbound.getWidth()*1000, (int)bbound.getHeight()*1000);
			}
			else if(finished) {
				finished = false;
				addPictures(recorder, meshRenderer, 20, (int)bbound.getWidth()*1000, (int)bbound.getHeight()*1000);
			}

			addPictures(recorder, meshRenderer, 5, (int)bbound.getWidth()*1000, (int)bbound.getHeight()*1000);


			/*try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
			overAllTime.resume();
			meshImprover.improve();
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
	                               MeshRenderer<AVertex, AHalfEdge, AFace> renderer,
	                               int frames,
	                               int width,
	                               int height) throws IOException {

		for(int i = 0; i < frames; i++) {
			recorder.addPicture(renderer.renderImage(width, height));
		}

	}
}
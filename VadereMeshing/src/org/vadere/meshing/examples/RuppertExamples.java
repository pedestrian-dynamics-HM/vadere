package org.vadere.meshing.examples;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPolygon;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class RuppertExamples {

	public static void main(String... args) throws InterruptedException, IOException {
		rupperts("/poly/mf_small_very_simple.poly");
	}

	public static void rupperts(@NotNull final String fileName) throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream(fileName);
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		Collection<VLine> lines = pslg.getAllSegments();
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();

		//IEdgeLengthFunction h = p -> 0.01 /*+ 0.2*Math.abs(distanceFunction.apply(p))*/;
		var ruppert = new PRuppertsTriangulator(
				pslg,
				p -> Double.POSITIVE_INFINITY,
				0,
				false
		);
		//(mesh, f -> false, width, height, colorFunction
		PMeshPanel panel = new PMeshPanel(ruppert.getMesh(), f -> ruppert.getMesh().getBooleanData(f, "boundary"),1000, 1000);
		panel.display("Ruppert's Algorithm");
		while (!ruppert.isFinished()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			synchronized (ruppert.getMesh()) {
				ruppert.step();
			}
			panel.repaint();
		}

		System.out.println(TexGraphGenerator.toTikz(ruppert.getMesh()));
		System.out.println("finished");
	}
}

package org.vadere.meshing.examples;

import org.vadere.meshing.mesh.gen.MeshRenderer;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.EdgeLengthFunctionApprox;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.utils.color.Colors;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

public class ElementSizeFunction {

	public static void main(String ... args) throws IOException {
		String fileName = "/poly/kaiserslautern.poly";
		final InputStream inputStream = ElementSizeFunction.class.getResourceAsStream("/poly/kaiserslautern.poly");

		System.out.println(String.format("Meshing %s...", fileName));

		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		System.out.println("minX = " + pslg.getAllPoints().stream().mapToDouble(p -> p.getX()).min().getAsDouble());
		System.out.println("minY = " + pslg.getAllPoints().stream().mapToDouble(p -> p.getY()).min().getAsDouble());
		System.out.println("maxX = " + pslg.getAllPoints().stream().mapToDouble(p -> p.getX()).max().getAsDouble());
		System.out.println("maxY = " + pslg.getAllPoints().stream().mapToDouble(p -> p.getY()).max().getAsDouble());
		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg);
		edgeLengthFunctionApprox.smooth(0.4);
		edgeLengthFunctionApprox.printPython();

		Function<PFace, Color> colorFunction = f -> {
			VTriangle triangle = edgeLengthFunctionApprox.getMesh().toTriangle(f);
			if(pslg.getHoles().stream().anyMatch(hole -> hole.contains(triangle.midPoint())) || !pslg.getSegmentBound().contains(triangle.midPoint())) {
				return Color.WHITE;
			} else {
				return Colors.YELLOW;
			}
		};

		System.out.println(TexGraphGenerator.toTikz(edgeLengthFunctionApprox.getMesh(), colorFunction, e -> Color.BLACK, v -> Color.BLACK, 1.0f, true));
		System.out.println(edgeLengthFunctionApprox.getMesh().getNumberOfVertices());

		double h0 = 1.0;
		var meshImprover = new PEikMesh(
				IDistanceFunction.create(pslg.getSegmentBound(), pslg.getHoles()),
				p -> edgeLengthFunctionApprox.apply(p),
				h0,
				pslg.getBoundingBox(),
				pslg.getAllPolygons()
		);

		var meshRenderer = new MeshRenderer<>(meshImprover.getMesh(), f -> false, f -> Color.WHITE, e -> Color.GRAY, v -> Color.BLACK);
		var meshPanel = new PMeshPanel(meshRenderer, 1000, 800);
		meshPanel.display("Combined distance functions " + h0);
		meshImprover.improve();
		int i = 1;
		while (!meshImprover.isFinished()) {
			synchronized (meshImprover.getMesh()) {
				meshImprover.improve();
			}
			//Thread.sleep(500);
			meshPanel.repaint();
			System.out.println(i + ":" + meshImprover.getQuality());
			i++;
		}

		System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh(), colorFunction, e -> Color.BLACK, v -> Color.BLACK, 1.0f, true));
	}

}

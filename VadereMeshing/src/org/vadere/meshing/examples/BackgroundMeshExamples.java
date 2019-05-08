package org.vadere.meshing.examples;

import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.triangulation.EdgeLengthFunctionApprox;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.meshing.utils.io.poly.PolyGenerator;
import org.vadere.util.geometry.shapes.VPoint;
import java.io.IOException;
import java.io.InputStream;

public class BackgroundMeshExamples {

	public static void main(String ... args) throws IOException, InterruptedException {
		greenlandLFS();
	}

	public static void greenlandLFS() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/kaiserslautern.poly");
		PSLG pslg = PolyGenerator.toPSLGtoVShapes(inputStream);
		double theta = 10;

		PRuppertsTriangulator<VPoint, Double, Double> ruppert = new PRuppertsTriangulator<>(
				pslg,
				p -> Double.POSITIVE_INFINITY,
				theta,
				(x, y) -> new VPoint(x, y));

		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg);
		edgeLengthFunctionApprox.printPython();

	}


}

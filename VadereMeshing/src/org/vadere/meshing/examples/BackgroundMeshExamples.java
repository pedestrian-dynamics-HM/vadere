package org.vadere.meshing.examples;

import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.triangulation.EdgeLengthFunctionApprox;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRuppertsTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PContrainedDelaunayTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PDelaunayTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

public class BackgroundMeshExamples {

	public static void main(String ... args) throws IOException, InterruptedException {
		corridorLFS();
	}

	public static void greenlandLFS() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/kaiserslautern.poly");
		PSLG pslg = PSLGGenerator.toPSLGtoVShapes(inputStream);
		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg);
		edgeLengthFunctionApprox.printPython();
	}

	public static void roomLFS() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/room.poly");
		PSLG pslg = PSLGGenerator.toPSLGtoVShapes(inputStream);
		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg, p -> 2.0);
		edgeLengthFunctionApprox.printPython();
	}

	public static void cornerLFS() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/corner.poly");
		PSLG pslg = PSLGGenerator.toPSLGtoVShapes(inputStream);
		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg, p -> 2.0);
		edgeLengthFunctionApprox.printPython();
	}

	public static void corridorLFS() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/narrowCorridor.poly");
		PSLG pslg = PSLGGenerator.toPSLGtoVShapes(inputStream);
		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg, p -> 2.0);
		edgeLengthFunctionApprox.printPython();
	}
}

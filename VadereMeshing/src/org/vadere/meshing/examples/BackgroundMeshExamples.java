package org.vadere.meshing.examples;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.triangulation.DistanceFunctionApproxBF;
import org.vadere.meshing.mesh.triangulation.EdgeLengthFunctionApprox;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRuppertsTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PContrainedDelaunayTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PDelaunayTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

public class BackgroundMeshExamples {

	private static final Color lightBlue = new Color(0.8584083044982699f, 0.9134486735870818f, 0.9645674740484429f);

	public static void main(String ... args) throws IOException, InterruptedException {
		//localFeatureSize("/poly/kaiserslautern.poly");
		//localFeatureSize("/poly/room.poly");
		//localFeatureSize("/poly/corner.poly");
		//localFeatureSize("/poly/narrowCorridor.poly");
		//localFeatureSize("/poly/bridge.poly");
		distance("/poly/mf_small_very_simple.poly");
		//distance("/poly/mf_small_very_simple.poly");
		//distance("/poly/mf_small_very_simple.poly");
	}

	public static void localFeatureSize(@NotNull final String fileName) throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream(fileName);
		PSLG pslg = PSLGGenerator.toPSLGtoVShapes(inputStream);
		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg);
		edgeLengthFunctionApprox.smooth(0.2);
		edgeLengthFunctionApprox.printPython();
		//System.out.println(TexGraphGenerator.toTikz(edgeLengthFunctionApprox.getMesh(), f-> lightBlue, 1.0f));
	}

	public static void distance(@NotNull final String fileName) throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream(fileName);
		PSLG pslg = PSLGGenerator.toPSLGtoVShapes(inputStream);
		DistanceFunctionApproxBF distFunctionApprox = new DistanceFunctionApproxBF(pslg, IDistanceFunction.create(pslg.getSegmentBound(), pslg.getHoles()));
		distFunctionApprox.printPython();
	}
}

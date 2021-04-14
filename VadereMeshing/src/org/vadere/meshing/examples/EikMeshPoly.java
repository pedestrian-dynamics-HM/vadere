package org.vadere.meshing.examples;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.MeshRenderer;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.triangulation.DistanceFunctionApproxBF;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.EdgeLengthFunctionApprox;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.meshing.utils.color.Colors;
import org.vadere.meshing.utils.io.poly.MeshPolyReader;
import org.vadere.meshing.utils.io.poly.MeshPolyWriter;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.io.*;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

public class EikMeshPoly {
	private static final Color lightBlue = new Color(0.8584083044982699f, 0.9134486735870818f, 0.9645674740484429f);

	public static void main(String... args) throws InterruptedException, IOException {
		if (args.length == 0) {
			System.out.println("Please provide \".obstacles.poly\" file!");
		}

		for (String fileName : args) {
			meshPoly(fileName);
		}
	}

	public static void meshPoly(@NotNull final String fileName) throws IOException, InterruptedException {
		final InputStream inputStream = new FileInputStream(new File(fileName));

		System.out.println(String.format("Meshing %s...", fileName));

		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg);
		edgeLengthFunctionApprox.smooth(0.4);
		edgeLengthFunctionApprox.printPython();

		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();
		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);
		IDistanceFunction distanceFunctionApproximation = new DistanceFunctionApproxBF(pslg, distanceFunction, () -> new PMesh());

		var ruppert = new PRuppertsTriangulator(
				pslg,
				p -> Double.POSITIVE_INFINITY,
				0,
				true
		);
		ruppert.generate();


		Collection<VPolygon> polygons = pslg.getAllPolygons();
		//polygons.add(targetShape);

		// (3) use EikMesh to improve the mesh
		double h0 = 5.0;
		var meshImprover = new PEikMesh(
				distanceFunctionApproximation,
				p -> edgeLengthFunctionApprox.apply(p),
				h0,
				pslg.getBoundingBox(),
				polygons
		);

		Function<PVertex, Color> vertexColorFunction = v -> {
			if(meshImprover.isSlidePoint(v)){
				return Colors.BLUE;
			} else if(meshImprover.isFixPoint(v)) {
				return Colors.RED;
			} else {
				return Color.BLACK;
			}
		};

		Predicate<PFace> alertPredicate = f ->{
			return !meshImprover.getMesh().isBoundary(f) && distanceFunction.apply(meshImprover.getMesh().toTriangle(f).midPoint()) > 0;
		};

		var meshRenderer = new MeshRenderer<>(meshImprover.getMesh(), f -> false, f -> Color.WHITE, e -> Color.GRAY, vertexColorFunction);
		var meshPanel = new PMeshPanel(meshRenderer, 1000, 800);
		meshPanel.display("Combined distance functions " + h0);
		meshImprover.improve();
		while (!meshImprover.isFinished()) {
			synchronized (meshImprover.getMesh()) {
				meshImprover.improve();
			}
			//Thread.sleep(500);
			meshPanel.repaint();
		}
		System.out.println(String.format("Mesh generation complete: %d vertices", meshImprover.getMesh().getNumberOfVertices()));

		System.out.println("Writing TikZ file...");
		write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(),  f-> lightBlue, null, vertexColorFunction,1.0f, true)), fileName + ".tex");
		System.out.println("Writing TikZ file finished");

		System.out.println("Writing Poly file...");
		MeshPolyWriter<PVertex, PHalfEdge, PFace> meshPolyWriter = new MeshPolyWriter<>();
		String[] splitName = fileName.split("\\.");
		write(meshPolyWriter.to2DPoly(meshImprover.getMesh()), fileName + "_tri.poly");
		System.out.println("Writing Poly file finished.");
	}

	public static void displayPolyFile(@NotNull final String fileName) throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream(fileName);
		MeshPolyReader<PVertex, PHalfEdge, PFace> meshPolyWriter = new MeshPolyReader<>(() -> new PMesh());
		var mesh = meshPolyWriter.readMesh(inputStream);
		var meshPanel = new PMeshPanel(mesh, 1000, 800);
		meshPanel.display("");
	}

	public static void fmmPolyFile(@NotNull final String fileName) throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream(fileName);
		MeshPolyReader<PVertex, PHalfEdge, PFace> meshPolyWriter = new MeshPolyReader<>(() -> new PMesh());
		var mesh = meshPolyWriter.readMesh(inputStream);
		var meshPanel = new PMeshPanel(mesh, 1000, 800);
		meshPanel.display("");
	}

	private static void write(final String string, final String filename) throws IOException {
		File outputFile = new File(filename);
		try(FileWriter fileWriter = new FileWriter(outputFile)) {
			fileWriter.write(string);
		}
	}

	private static String toTexDocument(final String tikz) {
		return "\\documentclass[usenames,dvipsnames]{standalone}\n" +
				"\\usepackage[utf8]{inputenc}\n" +
				"\\usepackage{amsmath}\n" +
				"\\usepackage{amsfonts}\n" +
				"\\usepackage{amssymb}\n" +
				"\\usepackage{calc}\n" +
				"\\usepackage{graphicx}\n" +
				"\\usepackage{tikz}\n" +
				"\\usepackage{xcolor}\n" +
				"\n" +
				"%\\clip (-0.200000,-0.100000) rectangle (1.2,0.8);\n" +
				"\\begin{document}"+
				tikz
				+
				"\\end{document}";
	}
}
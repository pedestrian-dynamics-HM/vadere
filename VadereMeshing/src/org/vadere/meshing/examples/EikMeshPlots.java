package org.vadere.meshing.examples;

import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.EdgeLengthFunctionApprox;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PDelaunayTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

/**
 * Script to construct Tikz-Plots for different edge length functions and geometries
 * which are used for the manuel and the documentation.
 */
public class EikMeshPlots {

	private static final Color lightBlue = new Color(0.8584083044982699f, 0.9134486735870818f, 0.9645674740484429f);

	public static void main(String... args) throws InterruptedException, IOException {
		/*discXAdaptive(0.02);
		randomDelaunay();
		uniformRing(0.05);
		uniformRing(0.3);
		uniformRing(0.1);
		eikMeshA(0.01);
		eikMeshA(0.05);
		eikMeshA(0.02);
		distanceFuncCombination(0.07);
		discSubtractRect(0.1);
		discSubtractRect(0.03);
		discSubtractRect(0.01);

		discSubtractRect2(0.1);
		discSubtractRect2(0.03);
		discSubtractRect2(0.01);

		kaiserslautern();
		ruppertsAndEikMeshKaiserslautern();*/
		//bridge();
		//roomLFS();
		//cornerLFS();

		//uniformRing(0.3);
		randomDelaunay();
	}

	public static void randomDelaunay() throws IOException, InterruptedException {
		ArrayList<EikMeshPoint> points = new ArrayList<>();
		Random random = new Random(1);
		for (int i = 0; i < 100; i++) {
			points.add(new EikMeshPoint(random.nextDouble() * 10, random.nextDouble() * 10));
		}

		PDelaunayTriangulator dt = new PDelaunayTriangulator(points);
		dt.generate();
		write(toTexDocument(TexGraphGenerator.toTikz(dt.getMesh(), f -> lightBlue, 1.0f)), "eikmesh_random_before");

		VPolygon bound = dt.getMesh().toPolygon(dt.getMesh().getBorder());
		var meshImprover = new PEikMesh(
				p -> 2.0,
				dt.getTriangulation()
		);

		// display the mesh
		PMeshPanel meshPanel = new PMeshPanel(dt.getMesh(), 1000, 1000);
		meshPanel.display("Random Delaunay triangulation");
		while (!meshImprover.isFinished()) {
			meshImprover.improve();
			Thread.sleep(10);
			meshPanel.repaint();
		}

		meshImprover.finish();
		meshPanel.repaint();

		var meshImprover2 = new PEikMesh(
				p -> 0.05 + 0.2 * Math.sqrt(p.getY() * p.getY() / 20.0 + p.getX() * p.getX() / 20.0),
				dt.getTriangulation(),
				true
		);

		while (!meshImprover2.isFinished()) {
			meshImprover2.improve();
			Thread.sleep(1);
			meshPanel.repaint();
			System.out.println("imp");
		}
		System.out.println("end");
		write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 1.0f)), "eikmesh_random_after");


	}

	public static void kaiserslautern() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/kaiserslautern.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();

		IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y);
		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);


		// (3) use EikMesh to improve the mesh
		double h0 = 5.0;
		var meshImprover = new PEikMesh(
				distanceFunction,
				p -> h0 + 0.3 * Math.abs(distanceFunction.apply(p)),
				h0,
				new VRectangle(segmentBound.getBounds2D()),
				pslg.getHoles()
		);

		meshImprover.generate();
		var meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800);
		write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 1.0f)), "eikmesh_kaiserslautern_" + Double.toString(h0).replace('.', '_'));

		// display the mesh
		meshPanel.display("Combined distance functions " + h0);
	}

	public static void bridge() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/bridge.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();
		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);

		// (3) use EikMesh to improve the mesh
		double h0 = 0.5;
		var meshImprover = new PEikMesh(
				distanceFunction,
				p -> h0 + 0.5 * Math.abs(distanceFunction.apply(p)),
				h0,
				new VRectangle(segmentBound.getBounds2D()),
				pslg.getHoles()
		);

		var meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800);
		meshPanel.display("Combined distance functions " + h0);
		while (!meshImprover.isFinished()) {
			meshImprover.improve();
			Thread.sleep(20);
			meshPanel.repaint();
		}
		//meshImprover.generate();

		write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 1.0f)), "eikmesh_kaiserslautern_" + Double.toString(h0).replace('.', '_'));

		// display the mesh
		meshPanel.display("Combined distance functions " + h0);
	}

	public static void roomLFS() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/room.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg, p -> 2.0);
		edgeLengthFunctionApprox.printPython();


		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();
		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);

		// (3) use EikMesh to improve the mesh
		double h0 = 0.5;
		var meshImprover = new PEikMesh(
				distanceFunction,
				edgeLengthFunctionApprox,
				h0,
				new VRectangle(segmentBound.getBounds2D()),
				pslg.getHoles()
		);

		var meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800);
		meshPanel.display("Combined distance functions " + h0);
		while (!meshImprover.isFinished()) {
			meshImprover.improve();
			Thread.sleep(20);
			meshPanel.repaint();
		}
		//meshImprover.generate();

		write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 1.0f)), "eikmesh_kaiserslautern_" + Double.toString(h0).replace('.', '_'));

		// display the mesh
		meshPanel.display("Combined distance functions " + h0);
	}

	public static void cornerLFS() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/corner.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg, p -> 1.0);
		edgeLengthFunctionApprox.printPython();

		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();
		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);

		// (3) use EikMesh to improve the mesh
		double h0 = 1.0;
		var meshImprover = new PEikMesh(
				distanceFunction,
				edgeLengthFunctionApprox,
				h0,
				new VRectangle(segmentBound.getBounds2D()),
				pslg.getHoles()
		);

		var meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800);
		meshPanel.display("Combined distance functions " + h0);
		while (!meshImprover.isFinished()) {
			meshImprover.improve();
			Thread.sleep(20);
			meshPanel.repaint();
		}
		//meshImprover.generate();

		write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 1.0f)), "eikmesh_kaiserslautern_" + Double.toString(h0).replace('.', '_'));

		// display the mesh
		meshPanel.display("Combined distance functions " + h0);
	}

	public static void eikMeshA(double h0) throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/a.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);

		PEikMesh meshImprover = new PEikMesh(pslg.getSegmentBound(), h0, pslg.getHoles());
		meshImprover.generate();
		write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 10.0f)), "eikmesh_a_" + Double.toString(h0).replace('.', '_'));

		// display the mesh
		var meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 1000);
		meshPanel.display("A");
	}

	public static void distanceFuncCombination(double h0) throws IOException {
		// define your holes
		VRectangle rect = new VRectangle(-0.5, -0.5, 1, 1);
		VRectangle boundary = new VRectangle(-1.5, -0.7, 3, 1.4);
		IDistanceFunction d1_c = IDistanceFunction.createDisc(-0.5, 0, 0.5);
		IDistanceFunction d2_c = IDistanceFunction.createDisc(0.5, 0, 0.5);
		IDistanceFunction d_r = IDistanceFunction.create(rect);
		IDistanceFunction d_b = IDistanceFunction.create(boundary);
		IDistanceFunction d_union = IDistanceFunction.union(IDistanceFunction.union(d1_c, d_r), d2_c);
		IDistanceFunction d = IDistanceFunction.substract(d_b, d_union);
		var meshImprover = new PEikMesh(
				d,
				p -> h0 + 0.3 * Math.abs(d.apply(p)),
				h0,
				GeometryUtils.boundRelative(boundary.getPath()),
				Arrays.asList(rect)
		);

		// generate the mesh
		meshImprover.generate();

		//System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh()));

		// (optional) define the gui to display the mesh
		var meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800);
		write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 10.0f)), "eikmesh_d_combined_" + Double.toString(h0).replace('.', '_'));

		// display the mesh
		meshPanel.display("Combined distance functions " + h0);
	}

	public static void discSubtractRect(double h0) throws IOException {
		// define your holes
		VRectangle rect = new VRectangle(-0.25, -0.25, 0.5, 0.5);
		VRectangle boundary = new VRectangle(-1.5, -0.7, 3, 1.4);
		IDistanceFunction d_c = IDistanceFunction.createDisc(0, 0, 0.5);
		IDistanceFunction d_r = IDistanceFunction.create(rect);
		IDistanceFunction d = IDistanceFunction.substract(d_c, d_r);
		var meshImprover = new PEikMesh(
				d,
				p -> h0 + 0.3 * Math.abs(d.apply(p)),
				h0,
				GeometryUtils.boundRelative(boundary.getPath()),
				Arrays.asList(rect)
		);

		// generate the mesh
		meshImprover.generate();

		//System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh()));

		// (optional) define the gui to display the mesh
		PMeshPanel meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800);
		write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 10.0f)), "eikmesh_disc_rect_non_uniform_" + Double.toString(h0).replace('.', '_'));

		// display the mesh
		meshPanel.display("Combined distance functions " + h0);
	}

	public static void discSubtractRect2(double h0) throws IOException {
		// define your holes
		VRectangle rect = new VRectangle(0, 0, 0.5, 0.5);
		VRectangle boundary = new VRectangle(-1.5, -0.7, 3, 1.4);
		IDistanceFunction d_c = IDistanceFunction.createDisc(0, 0, 0.5);
		IDistanceFunction d_r = IDistanceFunction.create(rect);
		IDistanceFunction d = IDistanceFunction.substract(d_c, d_r);
		var meshImprover = new PEikMesh(
				d,
				p -> h0 + 0.3 * Math.abs(d.apply(p)),
				h0,
				GeometryUtils.boundRelative(boundary.getPath()),
				Arrays.asList(rect)
		);

		// generate the mesh
		meshImprover.generate();

		//System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh()));

		// (optional) define the gui to display the mesh
		PMeshPanel meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800);
		write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 10.0f)), "eikmesh_disc_rect_sub_non_uniform_" + Double.toString(h0).replace('.', '_'));

		// display the mesh
		meshPanel.display("Combined distance functions " + h0);
	}

	public static void uniformRing(double h0) throws InterruptedException, IOException {
		// define a bounding box
		VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);

		// distance function that defines a disc with radius 1 at (1,1).
		VPoint center = new VPoint(1, 1);
		IDistanceFunction d = IDistanceFunction.createRing(1, 1, 0.2, 1.0);

		// define the EikMesh-Improver
		IEdgeLengthFunction h = p -> h0;
		PEikMesh meshImprover = new PEikMesh(
				d,
				h,
				Arrays.asList(center),
				h0,
				bound
		);


		// (optional) define the gui to display the mesh
		PMeshPanel meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800);
		meshPanel.display("Uniform ring " + h0);
		while (!meshImprover.isFinished()) {
			meshImprover.improve();
			Thread.sleep(10);
			meshPanel.repaint();
		}

		meshImprover.finish();
		meshPanel.repaint();
		// generate the mesh
		//meshImprover.generate();

		write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 10.0f)), "eikmesh_ring_uniform_" + Double.toString(h0).replace('.', '_'));

		// display the mesh
		meshPanel.display("Uniform ring " + h0);
	}

	public static void discXAdaptive(final double h0) throws IOException {
		// define a bounding box
		VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);

		// distance function that defines a ring with inner-radius 0.2 and outer-radius 1 at (1,1).
		IDistanceFunction ringDistance = IDistanceFunction.createDisc(1, 1, 1.0);


		// define the EikMesh-Improver
		IEdgeLengthFunction edgeLengthFunction = p -> h0 + 0.4 * Math.abs(p.getX() - 1);
		PEikMesh meshImprover = new PEikMesh(
				ringDistance,
				edgeLengthFunction,
				h0,
				bound);

		// (optional) define the gui to display the mesh
		var meshPanel = new MeshPanel<>(meshImprover.getMesh(), 1000, 800);

		// generate the mesh
		meshImprover.generate();

		write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 10.0f)), "eikmesh_disc_xadaptive_" + Double.toString(h0).replace('.', '_'));

		// display the mesh
		meshPanel.display("Adaptive disc");
	}

	public static void ruppertsAndEikMeshKaiserslautern() throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/a.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		Collection<VLine> lines = pslg.getAllSegments();
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();

		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);
		IEdgeLengthFunction h = p -> 0.01 /*+ 0.2*Math.abs(distanceFunction.apply(p))*/;
		var ruppert = new PRuppertsTriangulator(
				pslg,
				p -> 0.01,
				0.0
		);

		PMeshPanel panel = new PMeshPanel(ruppert.getMesh(), 1000, 1000);
		panel.display(" Voronoi Vertex Insertion");

		while (!ruppert.isFinished()) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			synchronized (ruppert.getMesh()) {
				ruppert.step();
			}
			panel.repaint();
		}

		var eikMesh = new PEikMesh(h, ruppert.getTriangulation());

		while (!eikMesh.isFinished()) {
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			synchronized (eikMesh.getMesh()) {
				eikMesh.improve();
			}
			panel.repaint();
		}

		System.out.println(TexGraphGenerator.toTikz(ruppert.getMesh()));
		System.out.println("finished");
	}

	private static void write(final String string, final String filename) throws IOException {
		File outputFile = new File("./eikmesh/" + filename + ".tex");
//		try(FileWriter fileWriter = new FileWriter(outputFile)) {
		//fileWriter.write(string);
//		}
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
				"\\begin{document}" +
				tikz
				+
				"\\end{document}";
	}

}
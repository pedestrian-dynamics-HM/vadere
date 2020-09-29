package org.vadere.meshing.examples;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.gen.MeshRenderer;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.DistanceFunctionApproxBF;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.EdgeLengthFunctionApprox;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.distmesh.Distmesh;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.AEikMesh;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PContrainedDelaunayTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PDelaunayTriangulator;
import org.vadere.meshing.utils.color.Colors;
import org.vadere.meshing.utils.io.IOUtils;
import org.vadere.meshing.utils.io.poly.MeshPolyWriter;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VDisc;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.math.IDistanceFunction;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class was used to generate many plots for my dissertation.
 */
public class MeshQuantityPrinting {

	public static void main(String... args) throws InterruptedException, IOException {
		//spaceFillingCurve2();
		//uniformMeshDiscFunction(0.10);
		//uniformMeshDiscFunctionDistMesh(0.05);
		//distMeshFail(0.05);
		//delaunyTri("/poly/a.poly");
		//eikMeshAirfoilPoly(0.005);
		//eikMeshSupermarket(0.25);
		//eikMeshKaiserslauternMittel(50000.0);
		randomDelaunay();
	}

	public static void randomDelaunay() throws IOException, InterruptedException {
		BufferedWriter meshWriter = null;
		ArrayList<IPoint> points = new ArrayList<>();
		Random random = new Random(1);
		for (int i = 0; i < 100; i++) {
			points.add(new VPoint(random.nextDouble() * 10, random.nextDouble() * 10));
		}

		PDelaunayTriangulator dt = new PDelaunayTriangulator(points);
		dt.generate();

		VPolygon bound = dt.getMesh().toPolygon(dt.getMesh().getBorder());
		var meshImprover = new PEikMesh(
				p -> 1.0,
				dt.getTriangulation()
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


		points.clear();
		points.addAll(meshImprover.getMesh().getBoundaryPoints());
		for (int i = 0; i < 100-points.size(); i++) {
			points.add(new VPoint(random.nextDouble() * 8, random.nextDouble() * 8));
		}

		dt = new PDelaunayTriangulator(points);
		dt.generate();

		bound = dt.getMesh().toPolygon(dt.getMesh().getBorder());
		var meshImprover2 = new PEikMesh(
				p -> 1.0,
				dt.getTriangulation()
		);


		vertexColorFunction = v -> {
			if(meshImprover2.isSlidePoint(v)){
				return Colors.BLUE;
			} else if(meshImprover2.isFixPoint(v)) {
				return Colors.RED;
			} else {
				return Color.BLACK;
			}
		};

		File dir = new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/eikmesh/");
		meshWriter = IOUtils.getWriter("randomDelaunay_before.tex", dir);
		meshWriter.write(TexGraphGenerator.toTikz(meshImprover2.getMesh(), f -> Colors.YELLOW, edge -> Color.BLACK, vertexColorFunction, 1.0f, true));
		meshWriter.close();

		PMeshPanel meshPanel2 = new PMeshPanel(meshImprover2.getMesh(), 1000, 1000);
		meshPanel2.display("Random Delaunay triangulation");
		while (!meshImprover2.isFinished()) {
			meshImprover2.improve();
			Thread.sleep(10);
			meshPanel2.repaint();
		}
		meshImprover2.finish();
		meshPanel2.repaint();

		dir = new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/eikmesh/");
		meshWriter = IOUtils.getWriter("randomDelaunay_after.tex", dir);
		meshWriter.write(TexGraphGenerator.toTikz(meshImprover2.getMesh(), f -> Colors.YELLOW, edge -> Color.BLACK, vertexColorFunction, 1.0f, true));
		meshWriter.close();

		BufferedWriter bufferedWriterQualities1 = IOUtils.getWriter("qualities1_eik.csv", dir);
		bufferedWriterQualities1.write("iteration quality\n");

		BufferedWriter bufferedWriterQualities2 = IOUtils.getWriter("qualities2_eik.csv", dir);
		bufferedWriterQualities2.write("iteration quality\n");

		BufferedWriter bufferedWriterAngles = IOUtils.getWriter("angles_eik.csv", dir);
		bufferedWriterAngles.write("iteration angle\n");

		bufferedWriterQualities1.write(printQualities(200, meshImprover2.getMesh(), f -> meshImprover.getTriangulation().faceToQuality(f)));
		bufferedWriterQualities1.close();

		bufferedWriterQualities2.write(printQualities(200, meshImprover2.getMesh(), f -> meshImprover.getTriangulation().faceToLongestEdgeQuality(f)));
		bufferedWriterQualities2.close();

		bufferedWriterAngles.write(printAngles(200, meshImprover2.getMesh()));
		bufferedWriterAngles.close();
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

		Function<PVertex, Color> vertexColorFunction = v -> {
			if(meshImprover.isSlidePoint(v)){
				return Colors.BLUE;
			} else if(meshImprover.isFixPoint(v)) {
				return Colors.RED;
			} else {
				return Color.BLACK;
			}
		};

		meshImprover.generate();
		var meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800);

		BufferedWriter meshWriter = null;
		File dir = new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/eikmesh/");
		meshWriter = IOUtils.getWriter("kaiserslautern.tex", dir);
		meshWriter.write(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> Colors.YELLOW, edge -> Color.BLACK, vertexColorFunction, 1.0f, true));
		meshWriter.close();

		BufferedWriter bufferedWriterQualities1 = IOUtils.getWriter("qualities1_eik.csv", dir);
		bufferedWriterQualities1.write("iteration quality\n");

		BufferedWriter bufferedWriterQualities2 = IOUtils.getWriter("qualities2_eik.csv", dir);
		bufferedWriterQualities2.write("iteration quality\n");

		BufferedWriter bufferedWriterAngles = IOUtils.getWriter("angles_eik.csv", dir);
		bufferedWriterAngles.write("iteration angle\n");

		bufferedWriterQualities1.write(printQualities(200, meshImprover.getMesh(), f -> meshImprover.getTriangulation().faceToQuality(f)));
		bufferedWriterQualities1.close();

		bufferedWriterQualities2.write(printQualities(200, meshImprover.getMesh(), f -> meshImprover.getTriangulation().faceToLongestEdgeQuality(f)));
		bufferedWriterQualities2.close();

		bufferedWriterAngles.write(printAngles(200, meshImprover.getMesh()));
		bufferedWriterAngles.close();

		// display the mesh
		meshPanel.display("Combined distance functions " + h0);
	}

	public static void eikMeshAirfoilPoly(double h0) throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/airfoil.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);

		VPolygon airfoil = pslg.getHoles().iterator().next();
		IEdgeLengthFunction e = p -> h0 + Math.abs(airfoil.distance(p)) * 0.2;

		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg, edge -> Double.POSITIVE_INFINITY, e);
		edgeLengthFunctionApprox.smooth(0.2);
		IDistanceFunction distanceFunction = IDistanceFunction.create(pslg.getSegmentBound(), pslg.getHoles());

		GenEikMesh<PVertex, PHalfEdge, PFace> meshImprover = new GenEikMesh<>(distanceFunction, edgeLengthFunctionApprox, h0, pslg.getBoundingBox(), pslg.getAllPolygons(), () -> new PMesh());
		var meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 1000);
		meshPanel.display("Airfoil");
		int it = 1;
		while (it < 500) {
			meshImprover.improve();
			it++;
			synchronized (meshImprover.getMesh()) {
				meshPanel.repaint();
			}
			Thread.sleep(10);
		}

		Function<PVertex, Color> vertexColorFunction = v -> {
			if(meshImprover.isSlidePoint(v)){
				return Colors.BLUE;
			} else if(meshImprover.isFixPoint(v)) {
				return Colors.RED;
			} else {
				return Color.BLACK;
			}
		};

		BufferedWriter meshWriter = null;
		try {
			File dir = new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/eikmesh/");
			BufferedWriter bufferedWriterQualities1 = IOUtils.getWriter("qualities1_eik.csv", dir);
			bufferedWriterQualities1.write("iteration quality\n");

			BufferedWriter bufferedWriterQualities2 = IOUtils.getWriter("qualities2_eik.csv", dir);
			bufferedWriterQualities2.write("iteration quality\n");

			BufferedWriter bufferedWriterAngles = IOUtils.getWriter("angles_eik.csv", dir);
			bufferedWriterAngles.write("iteration angle\n");

			bufferedWriterQualities1.write(printQualities(500, meshImprover.getMesh(), f -> meshImprover.getTriangulation().faceToQuality(f)));
			bufferedWriterQualities1.close();

			bufferedWriterQualities2.write(printQualities(500, meshImprover.getMesh(), f -> meshImprover.getTriangulation().faceToLongestEdgeQuality(f)));
			bufferedWriterQualities2.close();

			bufferedWriterAngles.write(printAngles(500, meshImprover.getMesh()));
			bufferedWriterAngles.close();

			meshWriter = IOUtils.getWriter("airfoil.tex", dir);
			meshWriter.write(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> Colors.YELLOW, edge -> Color.BLACK, vertexColorFunction, 1.0f, true));
			meshWriter.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		//meshImprover.generate();
		//write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 10.0f)), "eikmesh_airfoil_" + Double.toString(h0).replace('.', '_'));

		// display the mesh
	}

	public static void eikMeshSupermarket(double h0) throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/supermarket.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);

		double smoothness = 0.4;
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();
		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);
		IDistanceFunction distanceFunctionApproximation = new DistanceFunctionApproxBF(pslg, distanceFunction, () -> new PMesh());

		IEdgeLengthFunction edgeLengthFunction = p -> h0 + smoothness * Math.abs((distanceFunctionApproximation).apply(p));
		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg, edgeLengthFunction);
		edgeLengthFunctionApprox.smooth(smoothness);

		//EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg, edge -> Double.POSITIVE_INFINITY, e);
		//edgeLengthFunctionApprox.smooth(0.2);

		var meshImprover = new PEikMesh(
				distanceFunctionApproximation,
				edgeLengthFunction,
				h0,
				pslg.getBoundingBox(),
				pslg.getAllPolygons()
		);
		var meshPanel = new PMeshPanel(meshImprover.getMesh(), 1400, 1400);
		meshPanel.display("Supermarket");
		int it = 1;
		while (it < 500) {
			meshImprover.improve();
			it++;
			synchronized (meshImprover.getMesh()) {
				meshPanel.repaint();
			}
			Thread.sleep(10);
		}

		Function<PVertex, Color> vertexColorFunction = v -> {
			if(meshImprover.isSlidePoint(v)){
				return Colors.BLUE;
			} else if(meshImprover.isFixPoint(v)) {
				return Colors.RED;
			} else {
				return Color.BLACK;
			}
		};

		BufferedWriter meshWriter = null;
		try {
			File dir = new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/eikmesh/");
			BufferedWriter bufferedWriterQualities1 = IOUtils.getWriter("qualities1_eik.csv", dir);
			bufferedWriterQualities1.write("iteration quality\n");

			BufferedWriter bufferedWriterQualities2 = IOUtils.getWriter("qualities2_eik.csv", dir);
			bufferedWriterQualities2.write("iteration quality\n");

			BufferedWriter bufferedWriterAngles = IOUtils.getWriter("angles_eik.csv", dir);
			bufferedWriterAngles.write("iteration angle\n");

			bufferedWriterQualities1.write(printQualities(500, meshImprover.getMesh(), f -> meshImprover.getTriangulation().faceToQuality(f)));
			bufferedWriterQualities1.close();

			bufferedWriterQualities2.write(printQualities(500, meshImprover.getMesh(), f -> meshImprover.getTriangulation().faceToLongestEdgeQuality(f)));
			bufferedWriterQualities2.close();

			bufferedWriterAngles.write(printAngles(500, meshImprover.getMesh()));
			bufferedWriterAngles.close();

			meshWriter = IOUtils.getWriter("supermarket.tex", dir);
			meshWriter.write(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> Colors.YELLOW, edge -> Color.BLACK, vertexColorFunction, 1.0f, true));
			meshWriter.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		//meshImprover.generate();
		//write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 10.0f)), "eikmesh_airfoil_" + Double.toString(h0).replace('.', '_'));

		// display the mesh
	}

	public static void eikMeshKaiserslauternMittel(double h0) throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/kaiserslautern_mittel.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);

		double smoothness = 0.3;
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();
		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);
		IDistanceFunction distanceFunctionApproximation = new DistanceFunctionApproxBF(pslg, distanceFunction, () -> new PMesh());

		IEdgeLengthFunction edgeLengthFunction = p -> h0 + smoothness * Math.abs((distanceFunctionApproximation).apply(p));
		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg, edgeLengthFunction);
		edgeLengthFunctionApprox.smooth(smoothness);

		//EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg, edge -> Double.POSITIVE_INFINITY, e);
		//edgeLengthFunctionApprox.smooth(0.2);

		var meshImprover = new PEikMesh(
				distanceFunctionApproximation,
				edgeLengthFunctionApprox,
				h0,
				pslg.getBoundingBox(),
				pslg.getAllPolygons()
		);
		var meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800);
		meshPanel.display("Kaiserslautern");
		int it = 1;
		while (it < 500) {
			meshImprover.improve();
			it++;
			synchronized (meshImprover.getMesh()) {
				meshPanel.repaint();
			}
			Thread.sleep(10);
		}

		Function<PVertex, Color> vertexColorFunction = v -> {
			if(meshImprover.isSlidePoint(v)){
				return Colors.BLUE;
			} else if(meshImprover.isFixPoint(v)) {
				return Colors.RED;
			} else {
				return Color.BLACK;
			}
		};

		BufferedWriter meshWriter = null;
		try {
			File dir = new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/eikmesh/");
			BufferedWriter bufferedWriterQualities1 = IOUtils.getWriter("qualities1_eik.csv", dir);
			bufferedWriterQualities1.write("iteration quality\n");

			BufferedWriter bufferedWriterQualities2 = IOUtils.getWriter("qualities2_eik.csv", dir);
			bufferedWriterQualities2.write("iteration quality\n");

			BufferedWriter bufferedWriterAngles = IOUtils.getWriter("angles_eik.csv", dir);
			bufferedWriterAngles.write("iteration angle\n");

			bufferedWriterQualities1.write(printQualities(500, meshImprover.getMesh(), f -> meshImprover.getTriangulation().faceToQuality(f)));
			bufferedWriterQualities1.close();

			bufferedWriterQualities2.write(printQualities(500, meshImprover.getMesh(), f -> meshImprover.getTriangulation().faceToLongestEdgeQuality(f)));
			bufferedWriterQualities2.close();

			bufferedWriterAngles.write(printAngles(500, meshImprover.getMesh()));
			bufferedWriterAngles.close();

			meshWriter = IOUtils.getWriter("kaiserslautern_mittel.tex", dir);
			meshWriter.write(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> Colors.YELLOW, edge -> Color.BLACK, vertexColorFunction, 1.0f, true));
			meshWriter.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		//meshImprover.generate();
		//write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> lightBlue, 10.0f)), "eikmesh_airfoil_" + Double.toString(h0).replace('.', '_'));

		// display the mesh
	}

	private static void spaceFillingCurve() throws InterruptedException, IOException {
		double h0 = 0.2;
		VRectangle bound = new VRectangle(-2.1, -0.1, 8.2, 6.2);

		// distance function that defines a disc with radius 1 at (1,1).
		VPoint center = new VPoint(1,1);

		VRectangle rect = new VRectangle(0, 0, 4, 4);
		VDisc disc = new VDisc(new VPoint(0, 2), 2);
		VDisc disc2 = new VDisc(new VPoint(4, 2), 2);
		VDisc disc3 = new VDisc(new VPoint(2, 2), 1);
		IDistanceFunction drect = IDistanceFunction.create(rect);
		IDistanceFunction drect2 = IDistanceFunction.union(drect, p -> disc.distance(p));
		IDistanceFunction drect3 = IDistanceFunction.union(drect2, p -> disc2.distance(p));
		IDistanceFunction d = IDistanceFunction.substract(drect3, p -> disc3.distance(p));

		// define the EikMesh-Improver
		IEdgeLengthFunction h = p -> h0 + 0.5 * Math.abs(d.apply(p));
		List<VShape> constrains = new ArrayList<>();
		constrains.add(bound);

		AEikMesh meshImprover = new AEikMesh(
				d,
				h,
				h0,
				bound,
				Collections.EMPTY_LIST
		);
		//HSBtoRGB

		int partitions = 3;
		int numberOfFaces = 1924;
		Function<AFace, Color> faceColorFunction = f -> {
			int id = ((AFace)f).getId();
			//ColorHelper colorHelper = new ColorHelper(7400);
			int fac = numberOfFaces / partitions;
			float part = id / numberOfFaces;
			return new Color(Color.HSBtoRGB((id / fac) / ((float)partitions), 0.7f, 1.0f));
			//return colorHelper.numberToColor(id);
		};

		MeshRenderer<AVertex, AHalfEdge, AFace> meshRenderer = new MeshRenderer<>(meshImprover.getMesh(), f -> false, faceColorFunction);
		MeshPanel<AVertex, AHalfEdge, AFace> meshPanel = new MeshPanel<>(meshRenderer, 500, 500);
		meshPanel.display();

		while (!meshImprover.isFinished()) {
			Thread.sleep(10);
			meshImprover.improve();
			meshPanel.repaint();
			System.out.println(meshImprover.getMesh().getFaces().stream().mapToInt(f -> f.getId()).max().getAsInt());
		}

		BufferedWriter meshWriter = IOUtils.getWriter("spaceFillingCurve_partition_mesh.tex", new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/eikmesh/"));
		meshWriter.write(TexGraphGenerator.toTikz(meshImprover.getMesh(), faceColorFunction, e -> Color.BLACK, v -> Color.BLACK, 1.0f, true));
		meshWriter.close();
	}

	private static void spaceFillingCurve2() throws InterruptedException, IOException {
		String fileName = "/poly/kaiserslautern.poly";
		final InputStream inputStream = ElementSizeFunction.class.getResourceAsStream("/poly/bridge.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		IDistanceFunction d = IDistanceFunction.create(pslg.getSegmentBound(), pslg.getHoles());
		double h0 = 0.5;

		// define the EikMesh-Improver
		IEdgeLengthFunction h = p -> h0 + 0.4 * Math.abs(d.apply(p));
		List<VShape> constrains = new ArrayList<>();
		constrains.add(pslg.getSegmentBound());
		constrains.addAll(pslg.getHoles());

		AEikMesh meshImprover = new AEikMesh(
				d,
				h,
				h0,
				pslg.getBoundingBox(),
				constrains
		);
		//HSBtoRGB

		int partitions = 3;
		int numberOfFaces = 8376;
		Function<AFace, Color> faceColorFunction = f -> {
			int id = ((AFace)f).getId();
			//ColorHelper colorHelper = new ColorHelper(7400);
			int fac = numberOfFaces / partitions;
			float part = id / numberOfFaces;
			//return new Color(Color.HSBtoRGB((id / fac) / ((float)partitions), 0.7f, 1.0f));
			return new Color(Color.HSBtoRGB(((float)id) / numberOfFaces, 0.7f, 1.0f));
			//return colorHelper.numberToColor(id);
		};

		Function<AVertex, Color> vertexColorFunction = v -> {
			if(meshImprover.isFixPoint(v)) {
				return Colors.RED;
			} else if(meshImprover.isSlidePoint(v)) {
				return Colors.BLUE;
			}
			return Color.BLACK;
		};

		MeshRenderer<AVertex, AHalfEdge, AFace> meshRenderer = new MeshRenderer<>(meshImprover.getMesh(), f -> false, faceColorFunction, e -> Color.black, vertexColorFunction);
		MeshPanel<AVertex, AHalfEdge, AFace> meshPanel = new MeshPanel<>(meshRenderer, 1000, 1000);
		meshPanel.display();

		while (!meshImprover.isFinished()) {
			Thread.sleep(10);
			meshImprover.improve();
			meshPanel.repaint();
			System.out.println(meshImprover.getQuality());
			System.out.println(meshImprover.getMesh().getNumberOfFaces());
		}

		BufferedWriter meshWriter = IOUtils.getWriter("spaceFillingCurve_partition_mesh.tex", new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/eikmesh/"));
		meshWriter.write(TexGraphGenerator.toTikz(meshImprover.getMesh(), faceColorFunction, e -> Color.BLACK, v -> Color.BLACK, 1.0f, true));
		meshWriter.close();

		BufferedWriter writer = IOUtils.getWriter("bridge.poly", new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/eikmesh/"));
		MeshPolyWriter<AVertex, AHalfEdge, AFace> meshPolyWriter = new MeshPolyWriter<>();
		writer.write(meshPolyWriter.to2DPoly(meshImprover.getMesh()));
		writer.close();
	}

	private static void eikMeshQualities(
			@NotNull final PEikMesh meshImprover,
			final int maxIteration,
			@NotNull final LinkedList<Integer> meshPrints,
			@NotNull final File dir) throws IOException {
		Collections.sort(meshPrints);
		BufferedWriter bufferedWriterQualities1 = IOUtils.getWriter("qualities1_eik.csv", dir);
		bufferedWriterQualities1.write("iteration quality\n");

		BufferedWriter bufferedWriterQualities2 = IOUtils.getWriter("qualities2_eik.csv", dir);
		bufferedWriterQualities2.write("iteration quality\n");

		BufferedWriter bufferedWriterAngles = IOUtils.getWriter("angles_eik.csv", dir);
		bufferedWriterAngles.write("iteration angle\n");

		int init = 1;
		while (!meshImprover.isInitialized()) {
			meshImprover.initialize();
			BufferedWriter meshWriter = IOUtils.getWriter("mesh_int_" + init + ".tex", dir);
			meshWriter.write(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> Colors.YELLOW, e -> Color.BLACK, v -> Color.BLACK, 1.0f, true));
			meshWriter.close();
			init++;
		}

		var mesh = meshImprover.getMesh();
		var tri = meshImprover.getTriangulation();
		var panel = new PMeshPanel(meshImprover.getMesh(), 500, 500);
		panel.display();

		int iteration = 1;
		while (iteration < maxIteration+1) {
			meshImprover.improve();
			panel.repaint();
			bufferedWriterQualities1.write(printQualities(iteration, mesh, f -> tri.faceToQuality(f)));
			bufferedWriterQualities2.write(printQualities(iteration, mesh, f -> tri.faceToLongestEdgeQuality(f)));
			bufferedWriterAngles.write(printAngles(iteration, meshImprover.getMesh()));

			if(!meshPrints.isEmpty() && meshPrints.peek() == iteration) {
				meshPrints.poll();
				BufferedWriter meshWriter = IOUtils.getWriter("mesh_" + iteration + ".tex", dir);
				meshWriter.write(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> Colors.YELLOW, e -> Color.BLACK, v -> {

					if(meshImprover.isFixPoint(v)){
						return Colors.RED;
					}
					if(meshImprover.isSlidePoint(v)) {
						return Colors.BLUE;
					}
					return Color.BLACK;
				}, 1.0f, true));
				meshWriter.close();
			}

			iteration++;
		}

		bufferedWriterQualities1.close();
		bufferedWriterQualities2.close();
		bufferedWriterAngles.close();
	}

	private static void distMeshQualities(@NotNull final Distmesh distmesh,
	                                      final int maxIteration,
	                                      @NotNull final LinkedList<Integer> meshPrints,
	                                      @NotNull final File dir) throws IOException {
		Collections.sort(meshPrints);
		BufferedWriter bufferedWriterQualities1 = IOUtils.getWriter("qualities1_dist.csv", dir);
		bufferedWriterQualities1.write("iteration quality\n");

		BufferedWriter bufferedWriterIllegalEdges = IOUtils.getWriter("illegal_edges.csv", dir);
		bufferedWriterIllegalEdges.write("iteration illEdges\n");

		BufferedWriter bufferedWriterQualities2 = IOUtils.getWriter("qualities2_dist.csv", dir);
		bufferedWriterQualities2.write("iteration quality\n");

		BufferedWriter bufferedWriterAngles = IOUtils.getWriter("angles_dist.csv", dir);
		bufferedWriterAngles.write("iteration angle\n");

		int iteration = 1;
		while (iteration < maxIteration+1) {
			distmesh.improve();
			/*if(iteration + 1 == maxIteration+1) {
				distmesh.reTriangulate(true);
			}*/
			bufferedWriterIllegalEdges.write(iteration + " " + distmesh.getNumberOfIllegalTriangles() + "\n");
			bufferedWriterQualities1.write(printQualities(iteration, distmesh, f -> GeometryUtils.qualityOf(f)));
			bufferedWriterQualities2.write(printQualities(iteration, distmesh, f -> GeometryUtils.qualityLongestEdgeInCircle(f.p1, f.p2, f.p3)));
			bufferedWriterAngles.write(printAngles(iteration, distmesh));

			if(!meshPrints.isEmpty() && meshPrints.peek() == iteration) {
				meshPrints.poll();

				BufferedWriter meshWriter = IOUtils.getWriter("mesh_" + iteration + ".tex", dir);
				meshWriter.write(TexGraphGenerator.toTikz(distmesh.getTriangles(),  f -> Colors.YELLOW, e -> Color.BLACK, v -> Color.BLACK, 1.0f, true));
				meshWriter.close();
			}

			iteration++;
		}

		bufferedWriterIllegalEdges.close();;
		bufferedWriterQualities1.close();
		bufferedWriterQualities2.close();
		bufferedWriterAngles.close();
	}

	/**
	 * This examples shows how to mesh a geometry that is defined by a {@link org.vadere.util.math.IDistanceFunction}.
	 * The distance function has to be negative for all points inside the meshing area and positive for all other points.
	 * Furthermore the distance function should be differentiable. Here the distance functions defines a disc at (1,1) with radius 1.
	 * Additionally, the meshing algorithm requires a bound {@link VRectangle}, which contains the whole meshing area.
	 * The edgeLength is a measure for the approximate edge lengths of all edges since it is a uniform triangulation,
	 * i.e. the desired edge length function is a constant.
	 */
	public static void uniformMeshDiscFunction(double h0) throws InterruptedException, IOException {
		// define a bounding box
		VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);

		// distance function that defines a disc with radius 1 at (1,1).
		VPoint center = new VPoint(1,1);
		VRectangle hole = new VRectangle(0.5, 0.5, 1, 1);
		IDistanceFunction rect = IDistanceFunction.create(hole);
		IDistanceFunction d = IDistanceFunction.substract(IDistanceFunction.createDisc(center.x, center.y, 1.0), rect);

		// define the EikMesh-Improver
		IEdgeLengthFunction h = p -> h0 + 0.2 * Math.abs(d.apply(p));
		List<VShape> constrains = new ArrayList<>();
		constrains.add(bound);
		constrains.add(hole);

		PEikMesh meshImprover = new PEikMesh(
				d,
				h,
				h0,
				bound,
				constrains
		);

		LinkedList linkedList = Arrays.asList(1,4,7,50,100,150).stream().collect(Collectors.toCollection(LinkedList::new));

		eikMeshQualities(meshImprover, 150, linkedList, new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/eikmesh/"));

	}

	public static void uniformMeshDiscFunctionDistMesh(double h0) throws InterruptedException, IOException {
		// define a bounding box
		VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);

		// distance function that defines a disc with radius 1 at (1,1).
		VPoint center = new VPoint(1,1);
		VRectangle hole = new VRectangle(0.5, 0.5, 1, 1);
		IDistanceFunction rect = IDistanceFunction.create(hole);
		IDistanceFunction d = IDistanceFunction.substract(IDistanceFunction.createDisc(center.x, center.y, 1.0), rect);

		// define the EikMesh-Improver
		IEdgeLengthFunction h = p -> h0 + 0.2 * Math.abs(d.apply(p));
		Distmesh distmesh = new Distmesh(d, h, h0, bound, Collections.singleton(hole));

		LinkedList linkedList = Arrays.asList(1,4,7,50,100,150).stream().collect(Collectors.toCollection(LinkedList::new));

		distMeshQualities(distmesh, 150, linkedList, new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/distmesh/"));

	}

	public static void distMeshFail(double h0) throws InterruptedException, IOException {
		// define a bounding box
		VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);

		// distance function that defines a disc with radius 1 at (1,1).
		VPoint center = new VPoint(1,1);
		VRectangle hole = new VRectangle(0.5, 0.5, 1, 0.05);
		IDistanceFunction rect = IDistanceFunction.create(hole);
		IDistanceFunction d = IDistanceFunction.substract(IDistanceFunction.createDisc(center.x, center.y, 1.0), rect);

		// define the EikMesh-Improver
		//IEdgeLengthFunction h = p -> h0 + 0.2 * Math.min(Math.min(Math.min(new VPoint(0.5, 0.5).distance(p), new VPoint(0.5, 0.5+0.05).distance(p)), new VPoint(1.5, 0.5).distance(p)), new VPoint(1.5, 0.5+0.05).distance(p));
		IEdgeLengthFunction h = p -> h0 + 0.2 * Math.abs(d.apply(p));
		Distmesh distmesh = new Distmesh(d, h, h0, bound, Collections.singleton(hole));

		LinkedList linkedList = Arrays.asList(1,4,7,50,100,150).stream().collect(Collectors.toCollection(LinkedList::new));

		distMeshQualities(distmesh, 150, linkedList, new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/distmesh/"));

	}

	private static void delaunyTri(@NotNull final String fileName) throws IOException {
		InputStream in = MeshQuantityPrinting.class.getResourceAsStream(fileName);
		PSLG pslg = PSLGGenerator.toPSLG(in);

		//System.out.println(TexGraphGenerator.toTikz(pslg.getSegments(), pslg.getAllPolygons(), true, 1.0f));

		//PDelaunayTriangulator dt = new PDelaunayTriangulator(pslg.getAllPoints());
		//dt.generate(true);
		PContrainedDelaunayTriangulator dt = new PContrainedDelaunayTriangulator(pslg, true);
		dt.generate(true);
		System.out.println(TexGraphGenerator.toTikz(dt.getMesh(), f -> {
			VTriangle tri = dt.getMesh().toTriangle(f);
			if(pslg.getSegmentBound().contains(tri.midPoint()))
				return Colors.YELLOW;
			else
				return Color.WHITE;
		} , e -> Color.BLACK, null,1.0f, true));

		//System.out.println(printQualities(1, dt.getMesh(), f -> dt.getTriangulation().faceToQuality(f)));
	}

	private static String printQualities(int iteration, IMesh<PVertex, PHalfEdge, PFace> mesh, Function<PFace, Double> rho){
		StringBuilder builder = new StringBuilder();
		for(PFace face : mesh.getFaces()) {
			double quality = rho.apply(face);
			builder.append(iteration);
			builder.append(" ");
			builder.append(quality);
			builder.append("\n");
		}
		return builder.toString();
	}

	private static String printQualities(int iteration, Distmesh mesh, Function<VTriangle, Double> rho){
		StringBuilder builder = new StringBuilder();
		for(VTriangle triangle : mesh.getTriangles()) {
			double quality = rho.apply(triangle);
			builder.append(iteration);
			builder.append(" ");
			builder.append(quality);
			builder.append("\n");
		}
		return builder.toString();
	}

	private static String printAngles(int iteration, IMesh<PVertex, PHalfEdge, PFace> mesh){
		StringBuilder builder = new StringBuilder();
		for(PFace face : mesh.getFaces()) {

			for(PHalfEdge edge : mesh.getEdges(face)) {
				VPoint p1 = mesh.toPoint(edge);
				VPoint p2 = mesh.toPoint(mesh.getNext(edge));
				VPoint p3 = mesh.toPoint(mesh.getPrev(edge));
				builder.append(iteration);
				builder.append(" ");
				builder.append(GeometryUtils.angle(p1, p2, p3));
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	private static String printAngles(int iteration, Distmesh distmesh){
		StringBuilder builder = new StringBuilder();
		for(VTriangle face : distmesh.getTriangles()) {

			builder.append(iteration);
			builder.append(" ");
			builder.append(GeometryUtils.angle(face.p1, face.p2, face.p3));
			builder.append("\n");

			builder.append(iteration);
			builder.append(" ");
			builder.append(GeometryUtils.angle(face.p3, face.p1, face.p2));
			builder.append("\n");

			builder.append(iteration);
			builder.append(" ");
			builder.append(GeometryUtils.angle(face.p2, face.p3, face.p1));
			builder.append("\n");

		}
		return builder.toString();
	}
}

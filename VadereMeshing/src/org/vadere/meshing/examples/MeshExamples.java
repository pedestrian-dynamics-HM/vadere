package org.vadere.meshing.examples;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IMeshDistanceFunction;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.ADelaunayTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PContrainedDelaunayTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PVoronoiVertexInsertion;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PDelaunayTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PVoronoiSegmentInsertion;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.meshing.utils.io.movie.MovRecorder;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.data.cellgrid.IPotentialPoint;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MeshExamples {
	private static Logger logger = Logger.getLogger(MeshExamples.class);

	public static void main(String... args) throws InterruptedException, IOException {
		/*for(int i = 1; i <= 629; i++) {
			System.out.println(i + " " + i + " " + (i + 1));
		}*/
		//eikMeshRandom();
		//eikMeshGreenland();
		//ruppertsTriangulationKaiserslauternLarge();
		//ruppertsTriangulationPoly();
		//ruppertsTriangulationPolyGreenland();
//		delaunayTriangulation();
		//dirichletRefinment();
//		delaunayRefinment();
//		constrainedDelaunayTriangulation();
		//eikMeshKaiserslautern();
		//eikMeshKaiserslauternApprox();
		eikMeshA();
		//eikMeshEik();
	}

	public static void faceTest() throws InterruptedException {
		VPoint p1 = new VPoint(-1,0);
		VPoint p2 = new VPoint(1, 0);
		VPoint p3 = new VPoint(0, Math.sqrt(4-1));
		VPoint p4 = new VPoint(4,4);
		VPoint p5 = new VPoint(3,0);
		List<VPoint> points = new ArrayList<>();
		points.add(p1);
		points.add(p2);
		points.add(p3);
		points.add(p4);
		points.add(p5);

		VTriangle triangle = new VTriangle(p1, p2, p3);
		VPoint c = triangle.getCircumcenter();
		VLine line = new VLine(p2, p3);
		VPoint midpoint = line.midPoint();
		VPoint toC = midpoint.subtract(c).norm().scalarMultiply(triangle.getCircumscribedRadius());
		VPoint newPoint = midpoint.subtract(c).scalarMultiply(2).add(c).add(toC);

		points.add(newPoint);

		PDelaunayTriangulator delaunayTriangulation = new PDelaunayTriangulator(points);
		delaunayTriangulation.generate();

		System.out.println(TexGraphGenerator.toTikz(delaunayTriangulation.getMesh()));
		System.out.println(newPoint);
		System.out.println(triangle.getCircumcenter());
		VPoint ca = new VTriangle(p2,p3,p4).getCircumcenter();
		System.out.println("ca" + ca);
		System.out.println("cca" + new VTriangle(p2,p3,ca).getCircumcenter());
		System.out.println("ccar" + new VTriangle(p2,p3,ca).getCircumscribedRadius());
		System.out.println(midpoint);
		System.out.println(new VTriangle(newPoint, p2, p3).getCircumcenter());
		System.out.println(new VTriangle(newPoint, p2, p3).getCircumscribedRadius());
		PMeshPanel panel = new PMeshPanel(delaunayTriangulation.getMesh(), 500, 500);
		panel.display("A square mesh");
		panel.repaint();
	}

	public static void square() {
		var mesh = new PMesh();

		assert mesh.getNumberOfFaces() == 0;

		mesh.toFace(
				new VPoint(0,0),
				new VPoint(1, 0),
				new VPoint(1, 1),
				new VPoint(0, 1));

		assert mesh.getNumberOfFaces() == 1;

		var panel = new PMeshPanel(mesh, 500, 500);
		panel.display("A square mesh");
		panel.repaint();

		mesh.getNext(mesh.getEdge(mesh.getFace()));
		mesh.streamPoints(mesh.getBorder()).parallel();


		System.out.println(TexGraphGenerator.toTikz(mesh));
	}

	public static void delaunayTriangulation() {

		// (1) generate a point set
		Random random = new Random(0);
		int width = 10;
		int height = 10;
		int numberOfPoints = 100;
		Supplier<VPoint> supply = () -> new VPoint(random.nextDouble()*width, random.nextDouble()*height);
		Stream<VPoint> randomPoints = Stream.generate(supply);
		List<VPoint> points = randomPoints.limit(numberOfPoints).collect(Collectors.toList());

		// (2) compute the Delaunay triangulation
		var delaunayTriangulator = new ADelaunayTriangulator(points);
		var triangulation = delaunayTriangulator.generate();

		// \definecolor{mygreen}{RGB}{85,168,104}
		Color green = new Color(85, 168, 104);
		Color red = new Color(196,78,82);

		var face = triangulation.locateFace(new VPoint(5,5)).get();
		var mesh = triangulation.getMesh();
		var deletePoints = mesh.getVertices(face);
		var surroundingFaces = mesh.getFaces(deletePoints.get(0));
		var ringEdges = mesh
				.streamEdges(deletePoints.get(0))
				.map(edge -> mesh.getPrev(edge)).collect(Collectors.toList());
		/*System.out.println(TexGraphGenerator.toTikz(
				delaunayTriangulator.getMesh(),
				f -> surroundingFaces.contains(f) ? red : Color.WHITE,
				1.0f));*/




		//triangulation.remove(deletePoints.get(0));

		var face2 = triangulation.locateFace(new VPoint(5,5)).get();
		var list = ringEdges.stream().map(e -> mesh.getFace(e)).collect(Collectors.toList());
		surroundingFaces.addAll(list);
		System.out.println(TexGraphGenerator.toTikz(
				delaunayTriangulator.getMesh(),
		//		f -> surroundingFaces.contains(f) ? green : Color.WHITE,
				1.0f));


		String propertyName = "area";
		LinkedList<AHalfEdge> visitedEdges = delaunayTriangulator.generate().straightGatherWalk2D(5, 5, delaunayTriangulator.getMesh().getFace());
		for(AFace f : delaunayTriangulator.getMesh().getFaces()) {
			delaunayTriangulator.getMesh().setData(f, propertyName, delaunayTriangulator.getMesh().toTriangle(f).getArea());
		}

		double areaSum = delaunayTriangulator.getMesh().streamFaces().mapToDouble(f -> delaunayTriangulator.getMesh().getData(f, propertyName, Double.class).get()).sum();
		double averageArea = areaSum / delaunayTriangulator.getMesh().getNumberOfFaces();
		System.out.println("Triangulated area = " + areaSum);
		System.out.println("Average triangle area = " + averageArea);
		System.out.println("Area triangulated = " + (100 * (areaSum / (width * height))) + " %");


		VPoint q = delaunayTriangulator.getMesh().toTriangle(delaunayTriangulator.getMesh().getFace(visitedEdges.peekFirst())).midPoint();
		Set<AFace> faceSet = visitedEdges.stream().map(e -> delaunayTriangulator.getMesh().getFace(e)).collect(Collectors.toSet());

		//\definecolor{myred}{RGB}{196,78,82}


		/*System.out.println(TexGraphGenerator.toTikz(
				delaunayTriangulator.getMesh(),
				//f -> delaunayTriangulation.getMesh().toTriangle(f).isNonAcute() ? red : Color.WHITE,
				1.0f));*/


		//System.out.println(TexGraphGenerator.toTikz(delaunayTriangulator.getMesh(), f -> faceSet.contains(f) ? red : Color.WHITE, 1.0f, new VLine(q, new VPoint(5,5))));

		/*delaunayTriangulation.getMesh().locate(5, 5);

		var panel = new PMeshPanel<>(
				delaunayTriangulation.getMesh(),
				500,
				500,
				f -> delaunayTriangulation.getMesh().toTriangle(f).isNonAcute() ? red : Color.WHITE);
		panel.display("Delaunay triangulation");
		panel.repaint();*/


	}

	public static void constrainedDelaunayTriangulation() throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/a.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		Collection<VLine> constrains = pslg.getAllSegments();
		var cdt = new PContrainedDelaunayTriangulator(
				pslg,
				true);
		cdt.generate();


		Collection<VPoint> allPoints = new ArrayList<>(constrains.size() * 2);
		for(VLine line : constrains) {
			allPoints.add(line.getVPoint1());
			allPoints.add(line.getVPoint2());
		}

		var dt = new PDelaunayTriangulator(
				allPoints
		);
		dt.generate();

		Color green = new Color(85, 168, 104);
		Color red = new Color(196,78,82);
		var allConstrains = cdt.getConstrains();
		Function<PHalfEdge, Color> colorFunction = e -> allConstrains.contains(e) ? red : Color.GRAY;

		System.out.println(TexGraphGenerator.toTikz(cdt.getMesh(), f -> Color.WHITE, colorFunction, 1.0f, false));
		//System.out.println(TexGraphGenerator.toTikz(dt.getMesh(), 1.0f));


		PMeshPanel panel = new PMeshPanel(cdt.getMesh(), 1000, 1000);
		panel.display("A square mesh");
		panel.repaint();


	}

	public static void eikMeshRandom() throws IOException {
		ArrayList<EikMeshPoint> points = new ArrayList<>();
		Random random = new Random(0);
		for(int i = 0; i < 1000; i++) {
			points.add(new EikMeshPoint(random.nextDouble() * 10, random.nextDouble() * 10));
		}

		PDelaunayTriangulator dt = new PDelaunayTriangulator(points);
		dt.generate();

		Function<PFace, Color> colorFunction = f ->  {
			return !dt.getTriangulation().isValid(f) ? Color.RED : Color.WHITE;
			//return new Color(quality, quality, quality);
		};

		PMeshPanel panel = new PMeshPanel(dt.getMesh(), 1000, 1000);
		panel.display(" Voronoi Vertex Insertion");

		VPolygon bound = dt.getMesh().toPolygon(dt.getMesh().getBorder());
		var eikMesh = new PEikMesh(
				p -> 1.0 /*+ Math.abs(bound.distance(p))*/,
				dt.getTriangulation()
		);

		System.out.println(TexGraphGenerator.toTikz(eikMesh.getMesh()));
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

		//var recorder = new MovRecorder<>(eikMesh, panel.getMeshRenderer(), 1024, 800, eikMesh.getMesh().getBound());
		//recorder.record();
		//recorder.finish();

		//System.out.println(TexGraphGenerator.toTikz(eikMesh.getMesh()));
		//System.out.println("finished");
	}

	public static void ruppertsTriangulationKaiserslautern() throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/kaiserslautern.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		Collection<VLine> lines = pslg.getAllSegments();
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();

		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);
		IEdgeLengthFunction h = p -> 0.01 /*+ 0.2*Math.abs(distanceFunction.apply(p))*/;
		var ruppert = new PRuppertsTriangulator(
				pslg,
				//p -> 0.01,
				10.0
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

		/*var eikMesh = new PEikMeshGen<>(h, ruppert.getTriangulation());

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
		}*/

		System.out.println(TexGraphGenerator.toTikz(ruppert.getMesh()));
		System.out.println("finished");
	}

	public static void ruppertsTriangulationKaiserslauternLarge() throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/kaiserslautern_large.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		Collection<VLine> lines = pslg.getAllSegments();
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();

		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);
		IEdgeLengthFunction h = p -> 0.01 /*+ 0.2*Math.abs(distanceFunction.apply(p))*/;
		var ruppert = new PRuppertsTriangulator(
				pslg,
				//p -> 0.01,
				10.0
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

		/*var eikMesh = new PEikMeshGen<>(h, ruppert.getTriangulation());

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
		}*/

		System.out.println(TexGraphGenerator.toTikz(ruppert.getMesh()));
		System.out.println("finished");
	}

	public static void ruppertsTriangulationPoly() throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/a.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		Collection<VLine> segments = pslg.getAllSegments();
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();

		System.out.println(TexGraphGenerator.toTikz(segments));

		double theta = 20.0;
		var ruppert = new PRuppertsTriangulator(
				pslg,
				p -> Double.POSITIVE_INFINITY,
				theta
		);
		//cdt.generate();
		//ruppertsTriangulator.generate();


		Function<PFace, Color> colorFunction = f ->  {
			float quality = ruppert.getMesh().isBoundary(f) ? 1.0f : (float)GeometryUtils.qualityOf(ruppert.getMesh().toTriangle(f));
			return new Color(quality, quality, quality);
		};
		final var coinstrains = ruppert.getSegments();
		//System.out.println(TexGraphGenerator.toTikz(ruppert.getMesh(), colorFunction, 1.0f));
		PMeshPanel panel = new PMeshPanel(ruppert.getMesh(), 1000, 1000, f -> Color.WHITE, e -> coinstrains.contains(e) ? Color.RED : Color.GRAY);
		panel.display("Rupperts Algorithm");

		while (!ruppert.isFinished()) {
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			synchronized (ruppert.getMesh()) {
				ruppert.step();
			}
			panel.repaint();
		}
		System.out.println(TexGraphGenerator.toTikz(ruppert.getMesh(), colorFunction, 50.0f));
		System.out.println("finished");
	}

	public static void ruppertsTriangulationPolyGreenland() throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/greenland.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		double theta = 20;
		PRuppertsTriangulator ruppert = new PRuppertsTriangulator(
				pslg,
				p -> Double.POSITIVE_INFINITY,
				theta
		);


//		EdgeLengthFunctionApprox edgeLengthFunctionApprox = new EdgeLengthFunctionApprox(pslg);
//		edgeLengthFunctionApprox.printPython();

		System.out.println();
//		edgeLengthFunctionApprox.smooth(0.3);
//		edgeLengthFunctionApprox.printPython();

		Function<PFace, Color> colorFunction = f ->  {
			float quality = ruppert.getMesh().isBoundary(f) ? 1.0f : (float)GeometryUtils.qualityOf(ruppert.getMesh().toTriangle(f));
			return new Color(quality, quality, quality);
		};

		final var segments = ruppert.getSegments();

		PMeshPanel panel = new PMeshPanel(ruppert.getMesh(), 800, 800, f -> Color.WHITE, e -> segments.contains(e) ? Color.RED : Color.GRAY);
		panel.display("Rupperts Algorithm");

		while (!ruppert.isFinished()) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			synchronized (ruppert.getMesh()) {
				ruppert.step();
			}
			panel.repaint();
		}
		logger.info(TexGraphGenerator.toTikz(ruppert.getMesh(), colorFunction, 1.0f));
	}

	public static void ruppertsTriangulation() {

		// bounding polygon
		VPolygon boundingBox = GeometryUtils.toPolygon(
				new VCircle(new VPoint(15.0/2.0, 15.0/2.0), 14.0),
				100);

		// first polygon
		VPolygon house = GeometryUtils.toPolygon(
				new VPoint(1, 1),
				new VPoint(1, 5),
				new VPoint(3, 7),
				new VPoint(5,5),
				new VPoint(5,1));

		PSLG pslg = new PSLG(boundingBox, Arrays.asList(house));

		PRuppertsTriangulator ruppertsTriangulator = new PRuppertsTriangulator(
				pslg,
				20);

		ruppertsTriangulator.generate();


		Function<PFace, Color> colorFunction = f ->  {
			float quality = ruppertsTriangulator.getMesh().isBoundary(f) ? 1.0f : (float)GeometryUtils.qualityOf(ruppertsTriangulator.getMesh().toTriangle(f));
			return new Color(quality, quality, quality);
		};
		System.out.println(TexGraphGenerator.toTikz(ruppertsTriangulator.getMesh(), colorFunction, 1.0f));
		PMeshPanel panel = new PMeshPanel(ruppertsTriangulator.getMesh(), 1000, 1000);
		panel.display("Rupperts Algorithm");

		/*while (true) {
			synchronized (ruppertsTriangulator.getMesh()) {
				ruppertsTriangulator.step();
			}

			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}*/
	}

	/*@NotNull IDistanceFunction distanceFunc,
			@NotNull IEdgeLengthFunction edgeLengthFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound,
			@NotNull Collection<? extends VShape> obstacleShapes,
			@NotNull IPointConstructor<P> pointConstructor*/

	public static void eikMeshA() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/a.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);

//		IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y);

		// (1) construct background mesh distance function
//		IDistanceFunction approxDistanceFunction = IMeshDistanceFunction.createDistanceFunction(pslg);

		// (2) construct initial mesh using Ruppert's algorithm
		/*double minCircumRadius = 0.015;
		double c = 1.0;
		double theta = 20.0;
		Function<IPoint, Double> circumRadiusFunc = p -> minCircumRadius + c * Math.abs(approxDistanceFunction.apply(p));
		PRuppertsTriangulator<EikMeshPoint, Double, Double> ruppertsTriangulator = new PRuppertsTriangulator<>(
				pslg, circumRadiusFunc, theta, pointConstructor
		);

		Color green = new Color(85, 168, 104);
		Color red = new Color(196,78,82);
		Function<PFace<EikMeshPoint, Double, Double>, Color> colorFunction = f ->  {
			return !ruppertsTriangulator.getTriangulation().isValid(f) ? red : Color.WHITE;
			//return new Color(quality, quality, quality);
		};

		while (!ruppertsTriangulator.isFinished()) {
			ruppertsTriangulator.step();
			panel.repaint();
			Thread.sleep(20);

		}*/


		// (3) use EikMesh to improve the mesh
		//IDistanceFunction d = IDistanceFunction.create(pslg.getSegmentBound(), pslg.getHoles());
		PEikMesh meshImprover = new PEikMesh(pslg.getSegmentBound(), 0.04, pslg.getHoles());

		var panel = new PMeshPanel(meshImprover.getMesh(), 500, 500);
		panel.display("EikMesh");

		while (!meshImprover.isFinished()) {
			meshImprover.improve();
			panel.repaint();
			Thread.sleep(30);
		}
		meshImprover.finish();
		panel.repaint();
		System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh()));

		/*var recorder = new MovRecorder<>(meshImprover, panel.getMeshRenderer(), 1024, 800, meshImprover.getMesh().getBound());
		recorder.record();
		recorder.finish();*/
	}

	public static void eikMeshEik() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/eikmesh.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);

//		IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y);

		// (1) construct background mesh distance function
//		IDistanceFunction approxDistanceFunction = IMeshDistanceFunction.createDistanceFunction(pslg);

		// (2) construct initial mesh using Ruppert's algorithm
		/*double minCircumRadius = 0.015;
		double c = 1.0;
		double theta = 20.0;
		Function<IPoint, Double> circumRadiusFunc = p -> minCircumRadius + c * Math.abs(approxDistanceFunction.apply(p));
		PRuppertsTriangulator<EikMeshPoint, Double, Double> ruppertsTriangulator = new PRuppertsTriangulator<>(
				pslg, circumRadiusFunc, theta, pointConstructor
		);

		Color green = new Color(85, 168, 104);
		Color red = new Color(196,78,82);
		Function<PFace<EikMeshPoint, Double, Double>, Color> colorFunction = f ->  {
			return !ruppertsTriangulator.getTriangulation().isValid(f) ? red : Color.WHITE;
			//return new Color(quality, quality, quality);
		};

		while (!ruppertsTriangulator.isFinished()) {
			ruppertsTriangulator.step();
			panel.repaint();
			Thread.sleep(20);
		}*/


		// (3) use EikMesh to improve the mesh
		//IDistanceFunction d = IDistanceFunction.create(pslg.getSegmentBound(), pslg.getHoles());
		PEikMesh meshImprover = new PEikMesh(pslg.getSegmentBound(), 0.5, pslg.getHoles());

		var panel = new PMeshPanel(meshImprover.getMesh(), 1000, 1000);
		/*panel.display("EikMesh");

		while (!meshImprover.isFinished()) {
			meshImprover.improve();
			panel.repaint();
			Thread.sleep(30);
		}
		meshImprover.finish();
		panel.repaint();*/
		//System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh()));

		var recorder = new MovRecorder<>(meshImprover, panel.getMeshRenderer(), 1024, 800);
		recorder.record();
		recorder.finish();

	}

	public static void eikMeshKaiserslauternApprox() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/kaiserslautern.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();

		IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y);

		// (1) construct background mesh distance function
		//IDistanceFunction approxDistanceFunction = IMeshDistanceFunction.createDistanceFunction(boundingBox, holes);

		//IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);

		IDistanceFunction distanceFunction = IMeshDistanceFunction.createDistanceFunction(pslg);

		// (2) construct initial mesh using Ruppert's algorithm
		double minCircumRadius = 0.015;
		double c = 1.0;
		double theta = 25.0;
		Function<IPoint, Double> circumRadiusFunc = p -> minCircumRadius + c * Math.abs(distanceFunction.apply(p));

		PRuppertsTriangulator ruppertsTriangulator = new PRuppertsTriangulator(
				pslg, circumRadiusFunc, theta
		);

		Color green = new Color(85, 168, 104);
		Color red = new Color(196,78,82);


		/*while (!ruppertsTriangulator.isFinished()) {
			ruppertsTriangulator.step();
			panel.repaint();
			Thread.sleep(20);
		}*/

		System.out.println(new VRectangle(segmentBound.getBounds2D()));

		// (3) use EikMesh to improve the mesh
		double h0 = 5.0;
		PEikMesh meshImprover = new PEikMesh(
				distanceFunction,
				p -> h0 + 0.3 * Math.abs(distanceFunction.apply(p)),
				h0,
				new VRectangle(segmentBound.getBounds2D()),
				pslg.getHoles()
		);

		Function<PFace, Color> colorFunction = f ->  {
			//return !meshImprover.getTriangulation().isValid(f) ? red : Color.WHITE;
			//return new Color(quality, quality, quality);
			return Color.WHITE;
		};

		PMeshPanel panel = new PMeshPanel(meshImprover.getMesh(), 1000, 1000);
		panel.display("EikMesh");


		while (!meshImprover.isFinished()) {
			synchronized (meshImprover.getMesh()) {
				meshImprover.improve();
			}
			panel.repaint();
			Thread.sleep(10);
		}

		//logger.info(TexGraphGenerator.toTikz(meshImprover.getMesh()));
	}

	public static void eikMeshKaiserslautern() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/kaiserslautern.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();

		IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y);

		// (1) construct background mesh distance function
		//IDistanceFunction approxDistanceFunction = IMeshDistanceFunction.createDistanceFunction(boundingBox, holes);

		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);

		// (2) construct initial mesh using Ruppert's algorithm
		double minCircumRadius = 0.015;
		double c = 1.0;
		double theta = 25.0;
		Function<IPoint, Double> circumRadiusFunc = p -> minCircumRadius + c * Math.abs(distanceFunction.apply(p));

		PRuppertsTriangulator ruppertsTriangulator = new PRuppertsTriangulator(
				pslg, circumRadiusFunc, theta
		);

		Color green = new Color(85, 168, 104);
		Color red = new Color(196,78,82);


		/*while (!ruppertsTriangulator.isFinished()) {
			ruppertsTriangulator.step();
			panel.repaint();
			Thread.sleep(20);
		}*/

		System.out.println(new VRectangle(segmentBound.getBounds2D()));

		// (3) use EikMesh to improve the mesh
		double h0 = 5.0;
		PEikMesh meshImprover = new PEikMesh(
				distanceFunction,
				p -> h0 + 0.3 * Math.abs(distanceFunction.apply(p)),
				h0,
				new VRectangle(segmentBound.getBounds2D()),
				pslg.getHoles()
		);

		Function<PFace, Color> colorFunction = f ->  {
			//return !meshImprover.getTriangulation().isValid(f) ? red : Color.WHITE;
			//return new Color(quality, quality, quality);
			return Color.WHITE;
		};

		PMeshPanel panel = new PMeshPanel(meshImprover.getMesh(), 1000, 1000);
		/*panel.display("EikMesh");


		while (!meshImprover.isFinished()) {
			synchronized (meshImprover.getMesh()) {
				meshImprover.improve();
			}
			panel.repaint();
			Thread.sleep(10);
		}

		logger.info(TexGraphGenerator.toTikz(meshImprover.getMesh()));*/

		var recorder = new MovRecorder<>(meshImprover, panel.getMeshRenderer(), 1024, 800);
		recorder.record();
		recorder.finish();
	}

	public static void eikMeshGreenland() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/greenland.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		IPointConstructor<EikMeshPoint> pointConstructor = (x, y) -> new EikMeshPoint(x, y);

		// (1) construct background mesh distance function
		IDistanceFunction approxDistanceFunction = IMeshDistanceFunction.createDistanceFunction(pslg);

		// (2) construct initial mesh using Ruppert's algorithm
		//double minCircumRadius = 0.015;
		double c = 1.0;
		double theta = 20;
		//Function<IPoint, Double> circumRadiusFunc = p -> minCircumRadius + c * Math.abs(approxDistanceFunction.apply(p));

		PRuppertsTriangulator ruppertsTriangulator = new PRuppertsTriangulator(
				pslg, p -> Double.POSITIVE_INFINITY, theta
		);
		ruppertsTriangulator.generate();

		Color green = new Color(85, 168, 104);
		Color red = new Color(196,78,82);


		/*while (!ruppertsTriangulator.isFinished()) {
			ruppertsTriangulator.step();
			panel.repaint();
			Thread.sleep(20);
		}*/

		// (3) use EikMesh to improve the mesh
		var eikMesh = new PEikMesh(
				p -> 1.0 + 0.2*Math.abs(pslg.getSegmentBound().distance(p)),
				ruppertsTriangulator.getTriangulation()
		);

		Function<PFace, Color> colorFunction = f ->  {
			//return !meshImprover.getTriangulation().isValid(f) ? red : Color.WHITE;
			//return new Color(quality, quality, quality);
			return Color.WHITE;
		};

		PMeshPanel panel = new PMeshPanel(eikMesh.getMesh(), 1000, 1000, colorFunction);
		panel.display("EikMesh");


		while (!eikMesh.isFinished()) {
			synchronized (eikMesh.getMesh()) {
				eikMesh.improve();
			}
			panel.repaint();
			Thread.sleep(20);
			/*try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
		}

	}

	public static void dirichletRefinment() throws InterruptedException, IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/a.poly");
		boolean duplicatedLines = false;
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		Collection<VLine> lines = pslg.getAllSegments();
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();

		//IDistanceFunction distanceFunction = IDistanceFunction.create(boundingBox, hole);
		PVoronoiSegmentInsertion delaunayRefinement = new PVoronoiSegmentInsertion(
				pslg,
				(x, y) -> new EikMeshPoint(x, y),
				p -> 0.01);

		Function<PFace, Color> colorFunction = f ->  {
			float quality = delaunayRefinement.getMesh().isBoundary(f) ? 1.0f : (float)GeometryUtils.qualityOf(delaunayRefinement.getMesh().toTriangle(f));
			return new Color(quality, quality, quality);
		};

		PMeshPanel panel = new PMeshPanel(delaunayRefinement.getMesh(), 1000, 1000, colorFunction);
		panel.display("Dirichlet Refinement");

		while (!delaunayRefinement.isFinished()) {
			synchronized (delaunayRefinement.getMesh()) {
				delaunayRefinement.refine();
			}

			panel.repaint();
			Thread.sleep(100);
		}
		System.out.println(TexGraphGenerator.toTikz(delaunayRefinement.getMesh(), colorFunction, 10.0f));
		//panel.repaint();
	}

	public static void delaunayRefinment() throws InterruptedException, IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/a.poly");
		boolean duplicatedLines = false;
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		Collection<VLine> lines = pslg.getAllSegments();
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();

		var frontalMethod = new PVoronoiVertexInsertion(
				pslg,
				p -> 0.01);

		Function<PFace, Color> colorFunction = f ->  {
			float quality = frontalMethod.getMesh().isBoundary(f) ? 1.0f : (float)GeometryUtils.qualityOf(frontalMethod.getMesh().toTriangle(f));
			return new Color(quality, quality, quality);
		};

		PMeshPanel panel = new PMeshPanel(frontalMethod.getMesh(), 1000, 1000, colorFunction);
		panel.display("Delaunay Refinement");

		//frontalMethod.generate();
		synchronized (frontalMethod.getMesh()) {
			frontalMethod.refine();
		}
		while (!frontalMethod.isFinished()) {
			synchronized (frontalMethod.getMesh()) {
				frontalMethod.refine();
			}

			panel.repaint();
			Thread.sleep(10);
		}
		System.out.println("finished");
		System.out.println(TexGraphGenerator.toTikz(frontalMethod.getMesh(), colorFunction, 10.0f));
		panel.repaint();
	}


	private static boolean  isLowOfQuality(@NotNull final VTriangle triangle) {
		double alpha = 30; // lowest angle3D in degree
		double radAlpha = Math.toRadians(alpha);

		return GeometryUtils.angle(triangle.p1, triangle.p2, triangle.p3) < radAlpha
				|| GeometryUtils.angle(triangle.p3, triangle.p1, triangle.p2) < radAlpha
				|| GeometryUtils.angle(triangle.p2, triangle.p3, triangle.p1) < radAlpha;
	}

	public static class PotentialPoint extends EikMeshPoint implements IPotentialPoint {

		private double potential;
		private PathFindingTag tag;

		public PotentialPoint(double x, double y) {
			super(x, y, false);
			this.potential = Double.MAX_VALUE;
			this.tag = PathFindingTag.Undefined;
		}

		@Override
		public double getPotential() {
			return potential;
		}

		@Override
		public void setPotential(final double potential) {
			this.potential = potential;
		}

		@Override
		public void setPathFindingTag(@NotNull final PathFindingTag tag) {
			this.tag = tag;
		}

		@Override
		public PathFindingTag getPathFindingTag() {
			return tag;
		}
	}
}

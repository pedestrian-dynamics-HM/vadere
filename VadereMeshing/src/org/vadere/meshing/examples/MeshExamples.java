package org.vadere.meshing.examples;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PTriangulation;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.PEikMeshGen;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PContrainedDelaunayTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PVoronoiVertexInsertion;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PDelaunayTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PDirichletRefinement;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.meshing.utils.io.poly.PolyGenerator;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.data.cellgrid.IPotentialPoint;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.InterpolationUtil;

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

	public static void main(String... args) throws InterruptedException, IOException {
		ruppertsTriangulationPoly();
		//delaunayTriangulation();
		//dirichletRefinment();
		//delaunayRefinment();
		//constrainedDelaunayTriangulation();
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

		PDelaunayTriangulator<VPoint, Double, Double> delaunayTriangulation = new PDelaunayTriangulator<>(points, (x, y) -> new VPoint(x, y));
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
		PMeshPanel<VPoint, Double, Double> panel = new PMeshPanel<>(delaunayTriangulation.getMesh(), 500, 500);
		panel.display("A square mesh");
		panel.repaint();
	}

	public static void square() {
		var mesh = new PMesh<>((x, y) -> new VPoint(x, y));

		assert mesh.getNumberOfFaces() == 0;

		mesh.toFace(
				new VPoint(0,0),
				new VPoint(1, 0),
				new VPoint(1, 1),
				new VPoint(0, 1));

		assert mesh.getNumberOfFaces() == 1;

		var panel = new PMeshPanel<>(mesh, 500, 500);
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
		var delaunayTriangulator = new PDelaunayTriangulator<VPoint, Double, Double>(points, (x, y) -> new VPoint(x, y));
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


		LinkedList<PHalfEdge<VPoint, Double, Double>> visitedEdges = delaunayTriangulator.generate().straightGatherWalk2D(5, 5, delaunayTriangulator.getMesh().getFace());
		for(PFace<VPoint, Double, Double> f : delaunayTriangulator.getMesh().getFaces()) {
			delaunayTriangulator.getMesh().setData(f, delaunayTriangulator.getMesh().toTriangle(f).getArea());
		}

		double areaSum = delaunayTriangulator.getMesh().streamFaces().mapToDouble(f -> delaunayTriangulator.getMesh().getData(f).get()).sum();
		double averageArea = areaSum / delaunayTriangulator.getMesh().getNumberOfFaces();
		System.out.println("Triangulated area = " + areaSum);
		System.out.println("Average triangle area = " + averageArea);
		System.out.println("Area triangulated = " + (100 * (areaSum / (width * height))) + " %");


		VPoint q = delaunayTriangulator.getMesh().toTriangle(delaunayTriangulator.getMesh().getFace(visitedEdges.peekFirst())).midPoint();
		Set<PFace<VPoint, Double, Double>> faceSet = visitedEdges.stream().map(e -> delaunayTriangulator.getMesh().getFace(e)).collect(Collectors.toSet());

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
		boolean duplicatedLines = true;
		var vgeometry = PolyGenerator.toPSLGtoVShapes(inputStream, duplicatedLines);
		List<VLine> constrains = vgeometry.getRight();
		var cdt = new PContrainedDelaunayTriangulator<VPoint, Double, Double>(
						constrains,
						(x, y) -> new VPoint(x, y),
				true);
		cdt.generate();


		Collection<VPoint> allPoints = new ArrayList<>(constrains.size() * 2);
		for(VLine line : constrains) {
			allPoints.add(line.getVPoint1());
			allPoints.add(line.getVPoint2());
		}

		var dt = new PDelaunayTriangulator<VPoint, Double, Double>(
				allPoints,
				(x, y) -> new VPoint(x, y));
		dt.generate();

		Color green = new Color(85, 168, 104);
		Color red = new Color(196,78,82);
		var allConstrains = cdt.getConstrains();
		Function<PHalfEdge<VPoint, Double, Double>, Color> colorFunction = e -> allConstrains.contains(e) ? red : Color.GRAY;

		System.out.println(TexGraphGenerator.toTikz(cdt.getMesh(), f -> Color.WHITE, colorFunction, 1.0f));
		//System.out.println(TexGraphGenerator.toTikz(dt.getMesh(), 1.0f));


		PMeshPanel<VPoint, Double, Double> panel = new PMeshPanel<>(cdt.getMesh(), 1000, 1000);
		panel.display("A square mesh");
		panel.repaint();


	}

	public static void ruppertsTriangulationPoly() throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/a.poly");
		boolean duplicatedLines = false;
		var vgeometry = PolyGenerator.toPSLGtoVShapes(inputStream, duplicatedLines);
		List<VLine> lines = vgeometry.getRight();
		List<VPolygon> polygons = vgeometry.getLeft();
		VPolygon segmentBound = polygons.get(0);

		VPolygon boundingBox = vgeometry.getKey().get(0);
		VPolygon hole = vgeometry.getKey().get(1);
		if(!boundingBox.isCCW()) {
			boundingBox = boundingBox.revertOrder();
		}

		if(!hole.isCCW()) {
			hole = hole.revertOrder();
		}

		List<VPolygon> holes = Arrays.asList(hole);
		List<VLine> constrains = boundingBox.getLinePath();
		constrains.addAll(hole.getLinePath());

		System.out.println(TexGraphGenerator.toTikz(constrains));

		PRuppertsTriangulator<VPoint, Double, Double> ruppert = new PRuppertsTriangulator<>(
				boundingBox,
				holes,
				(x, y) -> new VPoint(x, y));
		//cdt.generate();
		//ruppertsTriangulator.generate();


		Function<PFace<VPoint, Double, Double>, Color> colorFunction = f ->  {
			float quality = ruppert.getMesh().isBoundary(f) ? 1.0f : (float)GeometryUtils.qualityOf(ruppert.getMesh().toTriangle(f));
			return new Color(quality, quality, quality);
		};
		final var segments = ruppert.getSegments();
		System.out.println(TexGraphGenerator.toTikz(ruppert.getMesh(), colorFunction, 1.0f));
		PMeshPanel<VPoint, Double, Double> panel = new PMeshPanel<>(ruppert.getMesh(), 1000, 1000, f -> Color.WHITE, e -> segments.contains(e) ? Color.RED : Color.GRAY);
		panel.display("Rupperts Algorithm");

		while (true) {
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


		PRuppertsTriangulator<VPoint, Double, Double> ruppertsTriangulator = new PRuppertsTriangulator<>(
						boundingBox,
						Arrays.asList(house),
						(x, y) -> new VPoint(x, y),
						20);

		ruppertsTriangulator.generate();


		Function<PFace<VPoint, Double, Double>, Color> colorFunction = f ->  {
			float quality = ruppertsTriangulator.getMesh().isBoundary(f) ? 1.0f : (float)GeometryUtils.qualityOf(ruppertsTriangulator.getMesh().toTriangle(f));
			return new Color(quality, quality, quality);
		};
		System.out.println(TexGraphGenerator.toTikz(ruppertsTriangulator.getMesh(), colorFunction, 1.0f));
		PMeshPanel<VPoint, Double, Double> panel = new PMeshPanel<>(ruppertsTriangulator.getMesh(), 1000, 1000);
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

	public static void ruppertsAndEikMesh() {

		// bounding polygon
		VPolygon boundingBox = GeometryUtils.toPolygon(
				new VCircle(new VPoint(15.0/2.0, 15.0/2.0), 20.0),
				100);

		// first polygon
		VPolygon house = GeometryUtils.toPolygon(
				new VPoint(1, 1),
				new VPoint(1, 5),
				new VPoint(3, 7),
				new VPoint(5,5),
				new VPoint(5,1));

		/*
		 * Constrcut distance function from a background grid.
		 */
		IDistanceFunction distanceFunction = IDistanceFunction.create(boundingBox, house);
		PMesh<IPotentialPoint, Double, Double> mesh = new PMesh<>((x, y) -> new PotentialPoint(x, y));
		PRuppertsTriangulator<IPotentialPoint, Double, Double> ruppertsTriangulator = new PRuppertsTriangulator<>(
				boundingBox,
				Arrays.asList(house),
				mesh,
				20,
				false);
		IIncrementalTriangulation<IPotentialPoint, Double, Double, PVertex<IPotentialPoint, Double, Double>, PHalfEdge<IPotentialPoint, Double, Double>, PFace<IPotentialPoint, Double, Double>> triangulation = ruppertsTriangulator.generate();
		for(IPotentialPoint point : mesh.getPoints()) {
			point.setPotential(distanceFunction.apply(point));
		}

		IDistanceFunction approxDistance = p -> InterpolationUtil.barycentricInterpolation(triangulation.getMesh().getPoints(triangulation.locateFace(p.getX(), p.getY()).get()), p.getX(), p.getY());

		PMeshPanel<IPotentialPoint, Double, Double> panel2 = new PMeshPanel<>(mesh, 1000, 1000);
		panel2.display("Background mesh");

		PMesh<EikMeshPoint, Double, Double> meshCopy = new PMesh<>((x, y) -> new EikMeshPoint(x, y, false));
		PRuppertsTriangulator<EikMeshPoint, Double, Double> ruppertsTriangulatorCopy = new PRuppertsTriangulator<>(
				boundingBox,
				Arrays.asList(house),
				meshCopy,
				20);
		ruppertsTriangulatorCopy.generate();


		Function<PFace<EikMeshPoint, Double, Double>, Color> colorFunction = f ->  {
			float quality = meshCopy.isBoundary(f) ? 1.0f : (float)GeometryUtils.qualityOf(meshCopy.toTriangle(f));
			return new Color(quality, quality, quality);
		};

		PMeshPanel<EikMeshPoint, Double, Double> panel = new PMeshPanel<>(meshCopy, 1000, 1000, colorFunction);
		panel.display("EikMesh and Ruppert's Algorithm");

		PEikMeshGen<EikMeshPoint, Double, Double> meshImprover = new PEikMeshGen<>(approxDistance, p -> 1.0 /*+ Math.abs(approxDistance.apply(p))*0.3*/, 2.0, new PTriangulation<>(meshCopy));
		while (true) {
			meshImprover.improve();
			panel.repaint();
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
		var vgeometry = PolyGenerator.toPSLGtoVShapes(inputStream, duplicatedLines);
		List<VLine> lines = vgeometry.getRight();
		List<VPolygon> polygons = vgeometry.getLeft();
		VPolygon segmentBound = polygons.get(0);

		VPolygon boundingBox = vgeometry.getKey().get(0);
		VPolygon hole = vgeometry.getKey().get(1);

		//IDistanceFunction distanceFunction = IDistanceFunction.create(boundingBox, hole);
		PDirichletRefinement<EikMeshPoint, Double, Double> delaunayRefinement = new PDirichletRefinement<>(
				boundingBox,
				Arrays.asList(hole),
				(x, y) -> new EikMeshPoint(x, y),
				p -> 0.01);

		Function<PFace<EikMeshPoint, Double, Double>, Color> colorFunction = f ->  {
			float quality = delaunayRefinement.getMesh().isBoundary(f) ? 1.0f : (float)GeometryUtils.qualityOf(delaunayRefinement.getMesh().toTriangle(f));
			return new Color(quality, quality, quality);
		};

		PMeshPanel<EikMeshPoint, Double, Double> panel = new PMeshPanel<>(delaunayRefinement.getMesh(), 1000, 1000, colorFunction);
		panel.display("Dirichlet Refinement");

		while (true) {
			synchronized (delaunayRefinement.getMesh()) {
				delaunayRefinement.refine();
			}

			panel.repaint();
			Thread.sleep(100);
		}
		//panel.repaint();
	}

	public static void delaunayRefinment() throws InterruptedException, IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/a.poly");
		boolean duplicatedLines = false;
		var vgeometry = PolyGenerator.toPSLGtoVShapes(inputStream, duplicatedLines);
		List<VLine> lines = vgeometry.getRight();
		List<VPolygon> polygons = vgeometry.getLeft();
		VPolygon segmentBound = polygons.get(0);

		VPolygon boundingBox = vgeometry.getKey().get(0);
		VPolygon hole = vgeometry.getKey().get(1);

		PVoronoiVertexInsertion<VPoint, Double, Double> frontalMethod = new PVoronoiVertexInsertion<>(
				boundingBox,
				Arrays.asList(hole),
				(x, y) -> new VPoint(x, y),
				p -> 0.01);

		Function<PFace<VPoint, Double, Double>, Color> colorFunction = f ->  {
			float quality = frontalMethod.getMesh().isBoundary(f) ? 1.0f : (float)GeometryUtils.qualityOf(frontalMethod.getMesh().toTriangle(f));
			return new Color(quality, quality, quality);
		};

		PMeshPanel<VPoint, Double, Double> panel = new PMeshPanel<>(frontalMethod.getMesh(), 1000, 1000, colorFunction);
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
		panel.repaint();
	}

	public static void ruppertsAndEikMeshShort() {

		// bounding polygon
		VPolygon boundingBox = GeometryUtils.toPolygon(
				new VCircle(new VPoint(15.0/2.0, 15.0/2.0), 20.0),
				100);

		// first polygon
		VPolygon house = GeometryUtils.toPolygon(
				new VPoint(1, 1),
				new VPoint(1, 5),
				new VPoint(3, 7),
				new VPoint(5,5),
				new VPoint(5,1));


		//IDistanceFunction distanceFunction = IDistanceFunction.create(boundingBox, Arrays.asList(house));
		PEikMeshGen<EikMeshPoint, Double, Double> meshImprover = new PEikMeshGen<>(
				p -> 1.0 /*+ Math.abs(approxDistance.apply(p))*0.3*/,
				1.0,
				boundingBox,
				Arrays.asList(house), (x, y) -> new EikMeshPoint(x, y));



		Function<PFace<EikMeshPoint, Double, Double>, Color> colorFunction = f ->  {
			float quality = meshImprover.getMesh().isBoundary(f) ? 1.0f : (float)GeometryUtils.qualityOf(meshImprover.getMesh().toTriangle(f));
			return new Color(quality, quality, quality);
		};

		PMeshPanel<EikMeshPoint, Double, Double> panel = new PMeshPanel<>(meshImprover.getMesh(), 1000, 1000, colorFunction);
		panel.display("EikMesh and Ruppert's Algorithm");


		meshImprover.initialize();
		while (true) {
			meshImprover.improve();
			panel.repaint();
			/*try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
		}
	}

	public static void ruppertsAndEikMeshShort2() {

		// bounding polygon
		VPolygon boundingBox = GeometryUtils.toPolygon(
				new VCircle(new VPoint(15.0/2.0, 15.0/2.0), 20.0),
				100);

		// first polygon
		VPolygon house = GeometryUtils.toPolygon(
				new VPoint(1, 1),
				new VPoint(1, 5),
				new VPoint(3, 7),
				new VPoint(5,5),
				new VPoint(5,1));

		IDistanceFunction distanceFunction = GenEikMesh.createDistanceFunction(boundingBox, Arrays.asList(house));
		PEikMeshGen<EikMeshPoint, Double, Double> meshImprover = new PEikMeshGen<>(
				distanceFunction,
				p -> 1.0 /*+ Math.abs(approxDistance.apply(p))*0.3*/,
				2.0,
				new VRectangle(boundingBox.getBounds2D()),
				Arrays.asList(house), (x, y) -> new EikMeshPoint(x, y));



		Function<PFace<EikMeshPoint, Double, Double>, Color> colorFunction = f ->  {
			float quality = meshImprover.getMesh().isBoundary(f) ? 1.0f : (float)GeometryUtils.qualityOf(meshImprover.getMesh().toTriangle(f));
			return new Color(quality, quality, quality);
		};

		PMeshPanel<EikMeshPoint, Double, Double> panel = new PMeshPanel<>(meshImprover.getMesh(), 1000, 1000, colorFunction);
		panel.display("EikMesh and Ruppert's Algorithm");


		int counter = 0;
		while (counter < 100) {
			counter++;
			meshImprover.improve();
			panel.repaint();
			/*try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
		}
		meshImprover.finish();
		panel.repaint();
	}

	public static void realWorldExample() {


	}


	private static boolean  isLowOfQuality(@NotNull final VTriangle triangle) {
		double alpha = 30; // lowest angle in degree
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

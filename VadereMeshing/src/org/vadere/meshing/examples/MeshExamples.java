package org.vadere.meshing.examples;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PTriangulation;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PContrainedDelaunayTriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PDelaunayTriangulation;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PRuppertsTriangulator;
import org.vadere.meshing.utils.tex.TexGraphGenerator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class MeshExamples {

	public static void main(String... args) {
		ruppertsTriangulation();
	}

	public static void square() {
		PMesh<VPoint, Integer, Integer> mesh = new PMesh<>((x, y) -> new VPoint(x, y));

		assert mesh.getNumberOfFaces() == 0;

		mesh.toFace(
				new VPoint(0,0),
				new VPoint(1, 0),
				new VPoint(1, 1),
				new VPoint(0, 1));

		assert mesh.getNumberOfFaces() == 1;

		PMeshPanel<VPoint, Integer, Integer> panel = new PMeshPanel<>(mesh, 500, 500);
		panel.display("A square mesh");
		panel.repaint();

		mesh.getNext(mesh.getEdge(mesh.getFace()));

		for(VPoint point : mesh.getPointIt(mesh.getBorder())) {

		}

		mesh.streamPoints(mesh.getBorder()).parallel();


		System.out.println(TexGraphGenerator.toTikz(mesh));
	}

	public static void delaunayTriangulation() {
		Random random = new Random(0);
		int width = 10;
		int height = 10;
		int numberOfPoints = 1000;

		List<VPoint> points = new ArrayList<>();
		for(int i = 0; i < numberOfPoints; i++) {
			points.add(new VPoint(random.nextDouble() * width, random.nextDouble() * height));
		}

		PDelaunayTriangulation<VPoint, Double, Double> delaunayTriangulation = new PDelaunayTriangulation<>(points, (x, y) -> new VPoint(x, y));
		delaunayTriangulation.generate();


		LinkedList<PHalfEdge<VPoint, Double, Double>> visitedEdges = delaunayTriangulation.generate().straightGatherWalk2D(5, 5, delaunayTriangulation.getMesh().getFace());

		for(PFace<VPoint, Double, Double> face : delaunayTriangulation.getMesh().getFaces()) {
			delaunayTriangulation.getMesh().setData(face, delaunayTriangulation.getMesh().toTriangle(face).getArea());
		}

		double areaSum = delaunayTriangulation.getMesh().streamFaces().mapToDouble(f -> delaunayTriangulation.getMesh().getData(f).get()).sum();
		double averageArea = areaSum / delaunayTriangulation.getMesh().getNumberOfFaces();
		System.out.println("Triangulated area = " + areaSum);
		System.out.println("Average triangle area = " + averageArea);
		System.out.println("Area triangulated = " + (100 * (areaSum / (width * height))) + " %");


		VPoint q = delaunayTriangulation.getMesh().toTriangle(delaunayTriangulation.getMesh().getFace(visitedEdges.peekFirst())).midPoint();
		Set<PFace<VPoint, Double, Double>> faceSet = visitedEdges.stream().map(e -> delaunayTriangulation.getMesh().getFace(e)).collect(Collectors.toSet());

		//System.out.println(TexGraphGenerator.toTikz(delaunayTriangulation.getMesh(), f -> faceSet.contains(f) ? Color.GREEN : Color.WHITE, 1.0f, new VLine(q, new VPoint(5,5))));

		delaunayTriangulation.getMesh().locate(5, 5);

		PMeshPanel<VPoint, Double, Double> panel = new PMeshPanel<>(delaunayTriangulation.getMesh(), 500, 500);
		panel.display("A square mesh");
		panel.repaint();


	}

	public static void constrainedDelaunayTriangulation() {
		Random random = new Random(0);
		int width = 10;
		int height = 10;
		int numberOfPoints = 1000;

		Set<VPoint> points = new HashSet<>(numberOfPoints);
		/*for(int i = 0; i < numberOfPoints; i++) {
			points.add(new VPoint(random.nextDouble() * width, random.nextDouble() * height));
		}*/


		List<VLine> constrains = new ArrayList<>();
		constrains.add(new VLine(2, 2, 8, 8));
		constrains.add( new VLine(3, 4, 5, 6));

		PContrainedDelaunayTriangulator<VPoint, Double, Double> cdt =
				new PContrainedDelaunayTriangulator<>(
						new VRectangle(0, 0, width, height),
						constrains,
						points,
						(x, y) -> new VPoint(x, y));
		cdt.generate();


		PMeshPanel<VPoint, Double, Double> panel = new PMeshPanel<>(cdt.getMesh(), 1000, 1000);
		panel.display("A square mesh");
		panel.repaint();


	}

	public static void ruppertsTriangulation() {
		int width = 15;
		int height = 15;

		// construct a bounding polygon containing all points
		/*VPolygon boundingBox = GeometryUtils.toPolygon(
				new VPoint(-5, -5),
				new VPoint(-5, 20),
				new VPoint(20, 20),
				new VPoint(20, -5));*/

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

		// second polygon
		/*VPolygon square = GeometryUtils.toPolygon(
				new VPoint(0, 0),
				new VPoint(0, 10),
				new VPoint(10, 10),
				new VPoint(10, 0));*/


		PRuppertsTriangulator<VPoint, Double, Double> ruppertsTriangulator = new PRuppertsTriangulator<>(
						boundingBox,
						Arrays.asList(house),
						(x, y) -> new VPoint(x, y),
						25);

		ruppertsTriangulator.generate();
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

	private static boolean  isLowOfQuality(@NotNull final VTriangle triangle) {
		double alpha = 30; // lowest angle in degree
		double radAlpha = Math.toRadians(alpha);

		return GeometryUtils.angle(triangle.p1, triangle.p2, triangle.p3) < radAlpha
				|| GeometryUtils.angle(triangle.p3, triangle.p1, triangle.p2) < radAlpha
				|| GeometryUtils.angle(triangle.p2, triangle.p3, triangle.p1) < radAlpha;
	}
}

package org.vadere.meshing.examples;

import org.vadere.meshing.mesh.gen.PMeshSuppliert;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.impl.PDelaunayTriangulator;
import org.vadere.meshing.utils.color.Colors;
import org.vadere.meshing.utils.io.movie.MovRecorder;
import org.vadere.meshing.utils.io.poly.MeshPolyReader;
import org.vadere.meshing.utils.io.poly.MeshPolyWriter;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Shows a very basic example how {@link GenEikMesh} can be used
 * to mesh a simple geometry.
 */
public class EikMeshExamples {
	private static final Color lightBlue = new Color(0.8584083044982699f, 0.9134486735870818f, 0.9645674740484429f);

	public static void main(String... args) throws InterruptedException, IOException {
		//squareHole2();
		//delaunayTriangulation();
		//distanceFuncCombination();
		uniformMeshDiscFunction(0.15);
		//uniformMeshRingFunction(0.05);
		//combineDistanceFunctions();
		//edgeLengthFunction();
		/*edgeLengthAndDistanceFunction();
		userDefinedPoints();*/
	}

	public static void squareHole() throws InterruptedException {

		IDistanceFunction distanceFunction = p -> Math.max(Math.abs(p.getX()-0.5), Math.abs(p.getY()-0.5)) - 0.5;

		var improver = new PEikMesh(distanceFunction, p -> 0.1, 0.01, new VRectangle(-2, -2, 4, 4));
		var panel = new PMeshPanel(improver.getMesh(), 500, 500);
		panel.display("A square mesh");
		panel.repaint();

		improver.initialize();
		for(int i = 0; i < 1000; i++) {
			Thread.sleep(50);
			improver.improve();
			panel.repaint();
		}

	}

	public static void squareHole2() throws InterruptedException {
		VRectangle rect = new VRectangle(0, 0, 1, 1);
		IDistanceFunction distanceFunction = p -> rect.distance(p);

		var improver = new PEikMesh(distanceFunction, p -> 0.1, 0.01, new VRectangle(-2, -2, 4, 4), Arrays.asList(rect));
		var panel = new PMeshPanel(improver.getMesh(), 500, 500);
		panel.display("A square mesh");
		panel.repaint();

		improver.initialize();
		for(int i = 0; i < 1000; i++) {
			Thread.sleep(50);
			improver.improve();
			panel.repaint();
		}

	}

	public static void delaunayTriangulation() throws InterruptedException {
		Random random = new Random(0);
		int width = 10;
		int height = 10;
		int numberOfPoints = 200;
		double linePoints = (int)Math.sqrt(numberOfPoints)+5;

		List<VPoint> points = new ArrayList<>();

		for(double i = 0; i < linePoints; i++) {
			points.add(new VPoint(0.1, 0.1 + i / linePoints * (height-0.2)));
			points.add(new VPoint(0.1 + i / linePoints * (width-0.2), 0.1));
			points.add(new VPoint(width-0.2, 0.1 + i / linePoints * (height-0.2)));
			points.add(new VPoint(0.1 + i / linePoints * (width-0.2), height-0.2));
		}

		for(int i = 0; i < numberOfPoints-15; i++) {
			points.add(new VPoint(1.5 + random.nextDouble() * (width-3), 1.5 + random.nextDouble() * (height-3)));
		}

		var delaunayTriangulator = new PDelaunayTriangulator(points);
		var triangulation = delaunayTriangulator.generate();

		var improver = new PEikMesh(p -> 1.0, triangulation);
		var panel = new PMeshPanel(triangulation.getMesh(), 500, 500);
		panel.display("A square mesh");
		panel.repaint();

		for(int i = 0; i < 1000; i++) {
			Thread.sleep(50);
			improver.improve();
			panel.repaint();
		}

	}

	/**
	 * This examples shows how to mesh a geometry that is defined by shapes ({@link VShape}), i.e. the boundary is
	 * defined by a rectangle ({@link VPolygon}) and holes (areas which are excluded from the actual meshing region)
	 * are defined by shapes, here a {@link VRectangle}. The edgeLength is a measure for the approximate edge lengths
	 * of all edges since it is a uniform triangulation, i.e. the desired edge length function is a constant.
	 */
	public static void uniformMeshShapes() {
		// define a bounding box
		/*VPolygon boundary = GeometryUtils.polygonFromPoints2D(
				new VPoint(0,0),
				new VPoint(0, 1),
				new VPoint(1, 2),
				new VPoint(2,1),
				new VPoint(2,0));*/

		// define your holes
		VRectangle rect = new VRectangle(0.5, 0.5, 1, 1);
		List<VShape> obstacleShapes = new ArrayList<>();
		obstacleShapes.add(rect);

		VPolygon boundary = GeometryUtils.polygonFromPoints2D(
				new VPoint(0,0),
				new VPoint(0, 1),
				new VPoint(1, 1),
				new VPoint(1,0));

		// define the EikMesh-Improver
		IDistanceFunction d_c = IDistanceFunction.createDisc(0.5, 0.5, 0.5);
		IDistanceFunction d_r = IDistanceFunction.create(rect);
		IDistanceFunction d = IDistanceFunction.substract(d_c, d_r);
		double edgeLength = 0.03;
		var meshImprover = new PEikMesh(
				d,
				p -> edgeLength + 0.5 * Math.abs(d.apply(p)),
				edgeLength,
				GeometryUtils.boundRelative(boundary.getPath()),
				Arrays.asList(rect)
		);

		// generate the mesh
		//meshImprover.generate();

		//System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh()));

		// (optional) define the gui to display the mesh
		PMeshPanel meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800);

		meshPanel.display("Geometry defined by shapes");
		meshImprover.initialize();
		meshPanel.repaint();

		while (!meshImprover.isFinished()) {
			meshImprover.improve();

			try {
				Thread.sleep(10 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			meshPanel.repaint();
		}
		System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh()));
		// display the mesh
		//meshPanel.display("Geometry defined by shapes");
	}

	public static void distanceFuncCombination() throws IOException {
		// define a bounding box
		/*VPolygon boundary = GeometryUtils.polygonFromPoints2D(
				new VPoint(0,0),
				new VPoint(0, 1),
				new VPoint(1, 2),
				new VPoint(2,1),
				new VPoint(2,0));*/

		// define your holes
		VRectangle rect = new VRectangle(-0.5, -0.5, 1, 1);
		VRectangle boundary = new VRectangle(-1.5,-0.7,3,1.4);
		IDistanceFunction d1_c = IDistanceFunction.createDisc(-0.5, 0, 0.5);
		IDistanceFunction d2_c = IDistanceFunction.createDisc(0.5, 0, 0.5);
		IDistanceFunction d_r = IDistanceFunction.create(rect);
		IDistanceFunction d_b = IDistanceFunction.create(boundary);
		IDistanceFunction d_union = IDistanceFunction.union(IDistanceFunction.union(d1_c, d_r), d2_c);
		IDistanceFunction d = IDistanceFunction.substract(d_b,d_union);
		double edgeLength = 0.07;
		var meshImprover = new PEikMesh(
				d,
				p -> edgeLength + 0.3 * Math.abs(d.apply(p)),
				edgeLength,
				boundary
		);

		// generate the mesh
		//meshImprover.generate();

		//System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh()));

		// (optional) define the gui to display the mesh
		PMeshPanel meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800);

		//var recorder = new MovRecorder<>(meshImprover, meshPanel.getMeshRenderer(), 1024, 800, meshImprover.getMesh().getBound());
		//recorder.record();

		meshPanel.display("Geometry defined by shapes");
		meshImprover.initialize();
		meshPanel.repaint();

		while (!meshImprover.isFinished()) {
			meshImprover.improve();

			try {
				Thread.sleep(10 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			meshPanel.repaint();
		}

		meshImprover.setDistanceFunc(d_b);
		meshImprover.setEdgeLenFunction(p -> edgeLength + 0.3 * Math.abs(d_b.apply(p)));
		//recorder.record();

		while (!meshImprover.isFinished()) {
			meshImprover.improve();

			try {
				Thread.sleep(10 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			meshPanel.repaint();
		}

		meshImprover.setDistanceFunc(d_r);
		meshImprover.setEdgeLenFunction(p -> edgeLength + 0.3 * Math.abs(d_r.apply(p)));
		//recorder.record();


		IDistanceFunction d_c = IDistanceFunction.createDisc(0, 0, 0.5);
		meshImprover.setDistanceFunc(d_c);
		meshImprover.setEdgeLenFunction(p -> edgeLength /*+ 0.3 * Math.abs(d_c.apply(p))*/);
		//recorder.record();
		while (!meshImprover.isFinished()) {
			meshImprover.improve();

			try {
				Thread.sleep(10 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			meshPanel.repaint();
		}

		//recorder.finish();

		Function<PVertex, Color> vertexColorFunction = v -> {
			if(meshImprover.getMesh().isAtBoundary(v)){
				return Colors.BLUE;
			} else if(meshImprover.isFixPoint(v)) {
				return Colors.RED;
			} else {
				return Color.BLACK;
			}
		};

		//System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh()));
		//write(toTexDocument(TexGraphGenerator.toTikz(meshImprover.getMesh(),  f-> lightBlue, null, vertexColorFunction,1.0f, true)), "mesh.tex");
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
		IDistanceFunction d = IDistanceFunction.createDisc(center.x, center.y, 1.0);



		// define the EikMesh-Improver
		IEdgeLengthFunction h = p -> h0 + 0.3 * Math.abs(d.apply(p));
		PEikMesh meshImprover = new PEikMesh(
				d,
				h,
				Arrays.asList(center),
				h0,
				bound
		);

		//meshImprover.setUseVirtualEdges(false);
		//meshImprover.setAllowEdgeSplits(false);
		//meshImprover.setAllowVertexCollapse(false);
		//meshImprover.generate();

		//System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh()));

		// (optional) define the gui to display the mesh
		//meshImprover.generate();


		var meshSuppliert = PMeshSuppliert.defaultMeshSupplier;

		var writer = new MeshPolyWriter<PVertex, PHalfEdge, PFace>();
		var reader = new MeshPolyReader<>(meshSuppliert);

		String polyString = writer.to2DPoly(meshImprover.getMesh());
		InputStream inputStream = new ByteArrayInputStream(polyString.getBytes(Charset.forName("UTF-8")));

		System.out.println(polyString);

		var mesh = reader.readMesh(inputStream);
		var meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800);

		// generate the mesh

		// display the mesh
		meshPanel.display("Geometry defined by a distance function (disc)");


		while (!meshImprover.isFinished()) {
			meshImprover.improve();
			//meshPanel.repaint();
			//Thread.sleep(100);
			//meshPanel.repaint();
		}

		Function<PVertex, Color> vertexColorFunction = v -> {
			if(meshImprover.isSlidePoint(v)){
				return Colors.GREEN;
			} else if(meshImprover.isFixPoint(v)) {
				return Colors.RED;
			} else {
				return Color.BLACK;
			}
		};

		System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> Colors.BLUE, e -> Color.BLACK, vertexColorFunction,1.0f, true));

		/*var recorder = new MovRecorder<>(meshImprover, meshPanel.getMeshRenderer(), 1024, 800, meshImprover.getMesh().getBound());
		recorder.record();
		recorder.finish();*/

		//System.out.println(PolyGenerator.to2DPoly(meshImprover.getMesh()));
	}

	/**
	 * This examples shows how to mesh a geometry that is defined by a {@link org.vadere.util.math.IDistanceFunction}.
	 * The distance function has to be negative for all points inside the meshing area and positive for all other points.
	 * Furthermore the distance function should be differentiable. Here the distance functions defines a ring at (1,1)
	 * with inner-radius 0.2, and outer-radius 1.0. Additionally, the meshing algorithm requires a bound {@link VRectangle},
	 * which contains the whole meshing area. The edgeLength is a measure for the approximate edge lengths of all edges
	 * since it is a uniform triangulation, i.e. the desired edge length function is a constant.
	 */
	public static void uniformMeshRingFunction(double h0) throws IOException {
		// define a bounding box
		VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);

		// distance function that defines a ring with inner-radius 0.2 and outer-radius 1 at (1,1).
		IDistanceFunction ringDistance = IDistanceFunction.createRing(1, 1, 0.2, 1.0);

		// define the EikMesh-Improver
		PEikMesh meshImprover = new PEikMesh(
				ringDistance,
				h0,
				bound);

		// (optional) define the gui to display the mesh
		var meshPanel = new MeshPanel<>(
				meshImprover.getMesh(), 1000, 800);

		// generate the mesh
		//meshImprover.generate();

		//System.out.println(PolyGenerator.to2DPoly(meshImprover.getMesh()));

		//System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh()));

		// display the mesh
		//meshPanel.display("Geometry defined by a distance function (ring)");

		var recorder = new MovRecorder<>(meshImprover, meshPanel.getMeshRenderer(), 1024, 800);
		recorder.record();
		recorder.finish();

	}

	/**
	 * This example illustrate how one can combine distance functions ({@link IDistanceFunction}). Here we subtract
	 * from a disc at (1, 1) with radius 1 another disc at (1, 1) with radius 0.2. The result is the a ring of
	 * example {@link EikMeshExamples#uniformMeshRingFunction}.
	 */
	public static void combineDistanceFunctions() {
		// define a bounding box
		VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);

		// distance function that defines a ring with inner-radius 0.2 and outer-radius 1 at (1,1).
		IDistanceFunction innerDisc = IDistanceFunction.createDisc(1, 1, 0.2);
		IDistanceFunction outerDisc = IDistanceFunction.createDisc(1, 1, 1.0);
		IDistanceFunction ringDistance = IDistanceFunction.substract(outerDisc, innerDisc);

		// define the EikMesh-Improver
		double edgeLength = 0.1;
		PEikMesh meshImprover = new PEikMesh(
				ringDistance,
				edgeLength,
				bound);

		// (optional) define the gui to display the mesh
		MeshPanel<PVertex, PHalfEdge, PFace> meshPanel = new MeshPanel<>(
				meshImprover.getMesh(), 1000, 800);

		// generate the mesh
		meshImprover.generate();

		// display the mesh
		meshPanel.display("Combination of distance functions");
	}

	/**
	 * This example is equal to {@link EikMeshExamples#uniformMeshRingFunction} but we use a so called
	 * desired relative edge length function. The minimum of the edge length function should be equals 1.0.
	 * The algorithm will produce edge length approximately as large as: edgeLength times disc_xadaptive.apply(p),
	 * where p is the midpoint of the edge. Here the edge length depend on the x-coordinate, i.e. edges to the right
	 * will be larger.
	 */
	public static void edgeLengthFunction() throws IOException {
		// define a bounding box
		VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);

		// distance function that defines a ring with inner-radius 0.2 and outer-radius 1 at (1,1).
		IDistanceFunction ringDistance = IDistanceFunction.createDisc(1, 1,  1.0);


		// define the EikMesh-Improver
		double edgeLength = 0.02;
		IEdgeLengthFunction edgeLengthFunction = p -> edgeLength + 0.4 * Math.abs(p.getX()-1);
		PEikMesh meshImprover = new PEikMesh(
				ringDistance,
				edgeLengthFunction,
				edgeLength,
				bound);

		// (optional) define the gui to display the mesh
		MeshPanel<PVertex, PHalfEdge, PFace> meshPanel = new MeshPanel<>(
				meshImprover.getMesh(), 1000, 800);

		// generate the mesh
		/*meshImprover.generate();

		// display the mesh
		meshPanel.display("Edge length function");*/

		var recorder = new MovRecorder<>(meshImprover, meshPanel.getMeshRenderer(), 1024, 800);
		recorder.record();
		recorder.finish();
	}

	/**
	 * This example is equal to {@link EikMeshExamples#edgeLengthFunction} but the desired relative edge length depends
	 * on the {@link IDistanceFunction}, i.e. we combine those two functions. Here we want the edges close to the boundary
	 * to be smaller. Since the distance function is negative inside the meshing area and the edge length function should never
	 * be smaller than 1 we add the absolute value of the distance. The factor variable controls how 'fast' the edge length
	 * will increase with the distance.
	 */
	public static void edgeLengthAndDistanceFunction() {
		// define a bounding box
		VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);

		// distance function that defines a ring with inner-radius 0.2 and outer-radius 1 at (1,1).
		IDistanceFunction ringDistance = IDistanceFunction.createRing(1, 1, 0.2, 1.0);

		final double factor = 6.0;
		IEdgeLengthFunction edgeLengthFunction = p -> 1.0 + factor * Math.abs(ringDistance.apply(p));

		// define the EikMesh-Improver
		double edgeLength = 0.06;
		PEikMesh meshImprover = new PEikMesh(
				ringDistance,
				edgeLengthFunction,
				edgeLength,
				bound);

		// (optional) define the gui to display the mesh
		MeshPanel<PVertex, PHalfEdge, PFace> meshPanel = new MeshPanel<>(
				meshImprover.getMesh(), 1000, 800);

		// generate the mesh
		meshImprover.generate();

		// display the mesh
		meshPanel.display("Distance dependent edge lengths");
	}

	/**
	 * This example is equal to {@link EikMeshExamples#edgeLengthAndDistanceFunction} but here we show how the user
	 * can introduce new data types. The user might want to define a new type of container / point to work with.
	 * This is easy to do due to the generic implementation of the meshing data structures and algorithms.
	 * First of all, new data type has to extends {@link EikMeshPoint}! Secondly, since the algorithm creates new points
	 * of an unknown data type to this implementation, it requires the information how such points can be created,
	 * i.e. it requires a {@link IPointConstructor} of points defined by the user. That is all!
	 */
	public static void userDefinedPoints() {

		/**
		 * Some user defined
		 */
		class MyPoint extends EikMeshPoint {

			private double value;

			public MyPoint(double x, double y, boolean fixPoint) {
				super(x, y, fixPoint);
				this.value = 0;
			}
		}

		// define a bounding box
		VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);

		// distance function that defines a ring with inner-radius 0.2 and outer-radius 1 at (1,1).
		IDistanceFunction ringDistance = IDistanceFunction.createRing(1, 1, 0.2, 1.0);

		final double factor = 6.0;
		IEdgeLengthFunction edgeLengthFunction = p -> 1.0 + factor * Math.abs(ringDistance.apply(p));

		// define the EikMesh-Improver
		double edgeLength = 0.05;

		// define your point constructor which will be used during the algorithm to create new points.
		IPointConstructor<MyPoint> pointConstructor = (x, y) -> new MyPoint(x, y, false);

		// like before but we have to add the point-constructor to the constructor of EikMesh and we use
		// the more generic type PEikMeshGen instead of PEikMes!
		PEikMesh meshImprover = new PEikMesh(
				ringDistance,
				edgeLengthFunction,
				edgeLength,
				bound
		);

		// (optional) define the gui to display the mesh
		MeshPanel<PVertex, PHalfEdge, PFace> meshPanel = new MeshPanel<>(
				meshImprover.getMesh(), 1000, 800);

		meshPanel.display("User defined Points");

		meshImprover.initialize();
		meshPanel.repaint();

		while (true) {
			meshImprover.improve();

			try {
				Thread.sleep(100 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			meshPanel.repaint();
		}

		// generate the mesh
		//meshImprover.generate();

		// display the mesh
		//meshPanel.display("User defined Points");
	}

	private static void write(final String string, final String filename) throws IOException {
		File outputFile = new File("./"+filename);
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

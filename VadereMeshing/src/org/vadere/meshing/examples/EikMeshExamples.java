package org.vadere.meshing.examples;

import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.EikMesh;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.PEikMeshGen;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.IDistanceFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a very basic example how {@link EikMesh} can be used
 * to mesh a simple geometry.
 */
public class EikMeshExamples {

	public static void main(String... args) {
		uniformMeshShapes();
		uniformMeshDiscFunction();
		uniformMeshRingFunction();
		combineDistanceFunctions();
		edgeLengthFunction();
		edgeLengthAndDistanceFunction();
		userDefinedPoints();
	}

	/**
	 * This examples shows how to mesh a geometry that is defined by shapes ({@link VShape}), i.e. the boundary is
	 * defined by a rectangle ({@link VPolygon}) and holes (areas which are excluded from the actual meshing region)
	 * are defined by shapes, here a {@link VRectangle}. The edgeLength is a measure for the approximate edge lengths
	 * of all edges since it is a uniform triangulation, i.e. the desired edge length function is a constant.
	 */
	public static void uniformMeshShapes() {
		// define a bounding box
		VPolygon boundary = GeometryUtils.polygonFromPoints2D(
				new VPoint(0,0),
				new VPoint(0, 1),
				new VPoint(1, 2),
				new VPoint(2,1),
				new VPoint(2,0));

		// define your holes
		VRectangle rect = new VRectangle(0.5, 0.5, 0.5, 0.5);
		List<VShape> obstacleShapes = new ArrayList<>();
		obstacleShapes.add(rect);

		// define the EikMesh-Improver
		double edgeLength = 0.1;
		PEikMesh meshImprover = new PEikMesh(
				boundary,
				edgeLength,
				obstacleShapes);

		// (optional) define the gui to display the mesh
		MeshPanel<EikMeshPoint, PVertex<EikMeshPoint>, PHalfEdge<EikMeshPoint>, PFace<EikMeshPoint>> meshPanel = new MeshPanel<>(
				meshImprover.getMesh(), 1000, 800,
				new VRectangle(boundary.getBounds()));

		// generate the mesh
		meshImprover.generate();

		// display the mesh
		meshPanel.display("Geometry defined by shapes");
	}

	/**
	 * This examples shows how to mesh a geometry that is defined by a {@link org.vadere.util.math.IDistanceFunction}.
	 * The distance function has to be negative for all points inside the meshing area and positive for all other points.
	 * Furthermore the distance function should be differentiable. Here the distance functions defines a disc at (1,1) with radius 1.
	 * Additionally, the meshing algorithm requires a bound {@link VRectangle}, which contains the whole meshing area.
	 * The edgeLength is a measure for the approximate edge lengths of all edges since it is a uniform triangulation,
	 * i.e. the desired edge length function is a constant.
	 */
	public static void uniformMeshDiscFunction() {
		// define a bounding box
		VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);

		// distance function that defines a disc with radius 1 at (1,1).
		IDistanceFunction discDistance = IDistanceFunction.createDisc(1, 1, 1.0);

		// define the EikMesh-Improver
		double edgeLength = 0.1;
		PEikMesh meshImprover = new PEikMesh(
				discDistance,
				edgeLength,
				bound);

		// (optional) define the gui to display the mesh
		MeshPanel<EikMeshPoint, PVertex<EikMeshPoint>, PHalfEdge<EikMeshPoint>, PFace<EikMeshPoint>> meshPanel = new MeshPanel<>(
				meshImprover.getMesh(), 1000, 800,
				bound);

		// generate the mesh
		meshImprover.generate();

		// display the mesh
		meshPanel.display("Geometry defined by a distance function (disc)");
	}

	/**
	 * This examples shows how to mesh a geometry that is defined by a {@link org.vadere.util.math.IDistanceFunction}.
	 * The distance function has to be negative for all points inside the meshing area and positive for all other points.
	 * Furthermore the distance function should be differentiable. Here the distance functions defines a ring at (1,1)
	 * with inner-radius 0.2, and outer-radius 1.0. Additionally, the meshing algorithm requires a bound {@link VRectangle},
	 * which contains the whole meshing area. The edgeLength is a measure for the approximate edge lengths of all edges
	 * since it is a uniform triangulation, i.e. the desired edge length function is a constant.
	 */
	public static void uniformMeshRingFunction() {
		// define a bounding box
		VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);

		// distance function that defines a ring with inner-radius 0.2 and outer-radius 1 at (1,1).
		IDistanceFunction ringDistance = IDistanceFunction.createRing(1, 1, 0.2, 1.0);

		// define the EikMesh-Improver
		double edgeLength = 0.1;
		PEikMesh meshImprover = new PEikMesh(
				ringDistance,
				edgeLength,
				bound);

		// (optional) define the gui to display the mesh
		MeshPanel<EikMeshPoint, PVertex<EikMeshPoint>, PHalfEdge<EikMeshPoint>, PFace<EikMeshPoint>> meshPanel = new MeshPanel<>(
				meshImprover.getMesh(), 1000, 800,
				bound);

		// generate the mesh
		meshImprover.generate();

		// display the mesh
		meshPanel.display("Geometry defined by a distance function (ring)");
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
		MeshPanel<EikMeshPoint, PVertex<EikMeshPoint>, PHalfEdge<EikMeshPoint>, PFace<EikMeshPoint>> meshPanel = new MeshPanel<>(
				meshImprover.getMesh(), 1000, 800,
				bound);

		// generate the mesh
		meshImprover.generate();

		// display the mesh
		meshPanel.display("Combination of distance functions");
	}

	/**
	 * This example is equal to {@link EikMeshExamples#uniformMeshRingFunction} but we use a so called
	 * desired relative edge length function. The minimum of the edge length function should be equals 1.0.
	 * The algorithm will produce edge length approximately as large as: edgeLength times edgeLengthFunction.apply(p),
	 * where p is the midpoint of the edge. Here the edge length depend on the x-coordinate, i.e. edges to the right
	 * will be larger.
	 */
	public static void edgeLengthFunction() {
		// define a bounding box
		VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);

		// distance function that defines a ring with inner-radius 0.2 and outer-radius 1 at (1,1).
		IDistanceFunction ringDistance = IDistanceFunction.createRing(1, 1, 0.2, 1.0);
		IEdgeLengthFunction edgeLengthFunction = p -> 1.0 + p.getX();

		// define the EikMesh-Improver
		double edgeLength = 0.05;
		PEikMesh meshImprover = new PEikMesh(
				ringDistance,
				edgeLengthFunction,
				edgeLength,
				bound);

		// (optional) define the gui to display the mesh
		MeshPanel<EikMeshPoint, PVertex<EikMeshPoint>, PHalfEdge<EikMeshPoint>, PFace<EikMeshPoint>> meshPanel = new MeshPanel<>(
				meshImprover.getMesh(), 1000, 800,
				bound);

		// generate the mesh
		meshImprover.generate();

		// display the mesh
		meshPanel.display("Edge length function");
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
		double edgeLength = 0.05;
		PEikMesh meshImprover = new PEikMesh(
				ringDistance,
				edgeLengthFunction,
				edgeLength,
				bound);

		// (optional) define the gui to display the mesh
		MeshPanel<EikMeshPoint, PVertex<EikMeshPoint>, PHalfEdge<EikMeshPoint>, PFace<EikMeshPoint>> meshPanel = new MeshPanel<>(
				meshImprover.getMesh(), 1000, 800,
				bound);

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
		PEikMeshGen<MyPoint> meshImprover = new PEikMeshGen<>(
				ringDistance,
				edgeLengthFunction,
				edgeLength,
				bound,
				pointConstructor);

		// (optional) define the gui to display the mesh
		MeshPanel<MyPoint, PVertex<MyPoint>, PHalfEdge<MyPoint>, PFace<MyPoint>> meshPanel = new MeshPanel<>(
				meshImprover.getMesh(), 1000, 800,
				bound);

		// generate the mesh
		meshImprover.generate();

		// display the mesh
		meshPanel.display("Distance dependent edge lengths");
	}
}

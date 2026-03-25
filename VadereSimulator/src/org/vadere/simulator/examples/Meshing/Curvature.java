package org.vadere.simulator.examples.Meshing;

import org.vadere.meshing.WeilerAtherton;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles.PTriangleMeshBuilder;
import org.vadere.meshing.mesh.gen.pointLocator.JumpAndWalkPointLocator;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.inter.mesh.ITriangleMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.MeshPythonUtils;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRegularRefinement;
import org.vadere.meshing.utils.io.IOUtils;
import org.vadere.meshing.utils.math.GeometryUtilsMesh;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.InterpolationUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Code to test the iterative eikonal solver discussed in the PhD thesis of B. Zoennchen (p. 187, Alg. 20).
 *
 * @author Benedikt Zoennchen
 */
public class Curvature {

	public static void main(String ... args) throws IOException, InterruptedException {
		adaptoveEikonalSolverSine();
	}

	public static void interativeEikonalSolver() throws InterruptedException, IOException {

		BufferedWriter meshWriter = IOUtils.getWriter("floorFields.txt", new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/"));

		VPolygon bound1 = new VRectangle(0, 0, 30, 30).toPolygon();
		VPolygon bound2 = new VRectangle(15, -1, 16, 16).toPolygon();
		VRectangle hole = new VRectangle(5, 12, 5, 10);
		WeilerAtherton weilerAtherton = new WeilerAtherton(Arrays.asList(bound1, bound2));
		VPolygon intersection = weilerAtherton.subtraction().get();

		IDistanceFunction distanceFunction = IDistanceFunction.create(intersection, hole);

		// (1) generate basic mesh
		var improver = new PEikMesh(
				distanceFunction,
				p -> 1.0 + Math.abs(distanceFunction.apply(p)) * 0.2,
				0.25,
				GeometryUtils.boundRelative(intersection.getPoints()),
				Arrays.asList(intersection, hole));

		improver.initialize();
		for(int i = 0; i < 100; i++) {
			//Thread.sleep(50);
			improver.improve();
			//panel.repaint();
		}

		/*for(PHalfEdge e : improver.getMesh().getEdges(improver.getMesh().getBorder())) {
			PHalfEdge edge = improver.getMesh().getNext(improver.getMesh().getTwin(e));
			if(improver.getMesh().isNonAcute(edge)) {
				improver.getTriangulation().splitEdge(improver.getMesh().getTwin(e), false);
			}
		}

		for(PFace h : improver.getMesh().getHoles()) {
			for(PHalfEdge e : improver.getMesh().getEdges(h)) {
				PHalfEdge edge = improver.getMesh().getNext(improver.getMesh().getTwin(e));
				if(improver.getMesh().isNonAcute(edge)) {
					improver.getTriangulation().splitEdge(improver.getMesh().getTwin(e), false);
				}
			}
		}*/


		var panel = new PMeshPanel(improver.getMesh(), 800, 800);
		panel.display("A square mesh");
		panel.repaint();

		var tri = improver.getTriangulation();
		for(int i = 0; i < 4; i++) {

			var curTri = tri;
			List<PVertex> list = tri.getMesh().vertices().getBoundaryVertices();
			Set<PVertex> initialVertices = new HashSet<>();
			initialVertices.addAll(list);
			list.stream().forEach(v -> curTri.getMesh().vertices().adjacentIterableFor(v).forEach(u -> initialVertices.add(u)));

			Set<PVertex> initialVertices2 = new HashSet<>();
			initialVertices.stream().forEach(v -> curTri.getMesh().vertices().adjacentIterableFor(v).forEach(u -> initialVertices2.add(u)));
			initialVertices2.addAll(initialVertices);

			ITriangleMeshWithDataStorage<PVertex, PHalfEdge, PFace> meshWithDataStorage = tri.getMeshBuilder().getMeshWithDataStorage();
			var solver = new MeshEikonalSolverFMM<PVertex, PHalfEdge, PFace>(
					p -> 1,
					//Arrays.asList(new VPoint(5, 5)),
					meshWithDataStorage,
					new JumpAndWalkPointLocator<>(tri.getMesh()),
					initialVertices2,
					p -> Math.abs(distanceFunction.apply(p)));
			solver.solve();
			System.out.println(solver.getPotential(5,5));
			meshWriter.write(MeshPythonUtils.toPythonTriangulation(tri.getMeshBuilder().getMeshWithDataStorage(), v -> solver.getPotential(v)));
			meshWriter.write("\n");


			double maxCurvature = 0;
			for(var v : tri.getMesh().vertices()) {
				double[] result = GeometryUtilsMesh.curvature(tri.getMesh(), v, vertex -> solver.getPotential(vertex));
				//System.out.println("Curvature: " + result[0]);
				//System.out.println("Gaussian curvature: " + result[1]);
				tri.getMeshDataStorage().setDoubleData(v, "curvature", result[0]);
				//if(!Double.isNaN(result[0])) {
					maxCurvature = Math.max(maxCurvature, result[0]);
				//}
			}
			System.out.println(maxCurvature);

			final double minEdgeLen = 0.1;
			final var pTri = tri;
			Predicate<PHalfEdge> edgePredicate = e -> {
				VLine line = pTri.getMesh().edges().toLine(e);
				double len = line.length();
				double x[] = new double[3];
				double y[] = new double[3];
				double z[] = new double[3];

				VPoint p = line.midPoint();
				var face = pTri.locateFace(p).get();
				pTri.getMesh().readConnectivity().getTriPoints(face, x, y, z, "curvature", pTri.getMeshDataStorage());
				double totalArea = GeometryUtils.areaOfPolygon(x, y);
				double curvature = InterpolationUtil.barycentricInterpolation(x, y, z, totalArea, p.getX(), p.getY());
				return len > minEdgeLen && curvature > 0.25;
			};

			var triangulation = IncrementalTriangulation.fromMeshBuilder(tri.getMeshBuilder().copy());
			GenRegularRefinement<PVertex, PHalfEdge, PFace> refinement = new GenRegularRefinement<>(triangulation, edgePredicate, i+1);
			refinement.setMaxLevel(i+1);
			refinement.refine();
			tri = triangulation;
		}

		meshWriter.close();


		/*final double mC = maxCurvature;
		Function<PVertex, Color> vertexColorFunction = v -> {
			double curvature = improver.getMesh().getDoubleData(v, "curvature");
			return new Color(Color.HSBtoRGB((float)(curvature / mC), 0.7f, 1.0f));
		};*/

		/*var panel = new PMeshPanel(improver.getMesh(), 1000, 1000, f -> Colors.YELLOW, e -> Color.BLACK, vertexColorFunction);
		panel.display("A square mesh");
		panel.repaint();*/
		//System.out.println(improver.getMesh().toPythonTriangulation(v -> solver.getPotential(v)));





		//while (!refinement.isFinished()) {
		//Thread.sleep(2000);
		//synchronized (triangulation.getMesh()) {

		//}
		//meshPanel.repaint();
		//}
		//meshPanel.repaint();

		/*PMeshPanel meshPanel3 = new PMeshPanel(triangulation.getMesh().clone(), 1000, 1000);
		meshPanel3.display("Refined mesh");

		var solver2 = new EikonalSolverFMMTriangulation<>(
				p -> 1,
				Arrays.asList(new VPoint(5,5)),
				triangulation);
		solver2.solve();
		System.out.println(triangulation.getMesh().toPythonTriangulation(v -> solver.getPotential(v)));*/

	}

	public static void adaptoveEikonalSolver() throws InterruptedException, IOException {
		BufferedWriter meshWriter = IOUtils.getWriter("floorField_ad.txt", new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/"));

		VPolygon bound1 = new VRectangle(0, 0, 30, 30).toPolygon();
		VPolygon bound2 = new VRectangle(15, -1, 16, 16).toPolygon();
		VRectangle hole = new VRectangle(5, 12, 5, 10);
		WeilerAtherton weilerAtherton = new WeilerAtherton(Arrays.asList(bound1, bound2));
		VPolygon intersection = weilerAtherton.subtraction().get();

		IDistanceFunction distanceFunction = IDistanceFunction.create(intersection, hole);

		// (1) generate basic mesh
		var improver = new PEikMesh(
				distanceFunction,
				p -> 1.0 + Math.abs(distanceFunction.apply(p)) * 0.2,
				0.25,
				GeometryUtils.boundRelative(intersection.getPoints()),
				Arrays.asList(intersection, hole));

		improver.initialize();
		for(int i = 0; i < 100; i++) {
			//Thread.sleep(50);
			improver.improve();
			//panel.repaint();
		}

		var boundaryEdges = improver.getTriangulation().getMesh().edges().getBoundaryEdges();
		for(var e : boundaryEdges) {
			var edge = improver.getTriangulation().getMesh().edges().getTwin(e);
			VPoint p1 = improver.getTriangulation().getMesh().edges().endToPoint(edge);
			VPoint p2 = improver.getTriangulation().getMesh().edges().endToPoint(improver.getTriangulation().getMesh().edges().getNext(edge));
			VPoint p3 = improver.getTriangulation().getMesh().edges().endToPoint(improver.getTriangulation().getMesh().edges().getPrev(edge));

			double angle1 = GeometryUtils.angle(p1, p2, p3);

			// non-acute triangle
			double rightAngle = Math.PI/2;
			if(angle1 > rightAngle + GeometryUtils.DOUBLE_EPS) {
				improver.getTriangulation().getMeshBuilder().changeConnectivity().splitEdge(edge, false);
			}
		}

		ArrayList<IPoint> points = new ArrayList<>();
		points.add(new VPoint(5, 5));
		points.add(new VPoint(25, 25));

		var solver = new MeshEikonalSolverFMM<>(
				new UnitTimeCostFunction(),
				improver.getTriangulation().getMeshBuilder().getMeshWithDataStorage(),
				new JumpAndWalkPointLocator<>(improver.getMesh()),
				//points,
				improver.getTriangulation().getMesh().vertices().getBoundaryVertices(),
				p -> 0.0);
		solver.solve();

		meshWriter.write(MeshPythonUtils.toPythonTriangulation(solver.getMeshWithDataStorage(), v -> solver.getPotential(v)));
		meshWriter.close();
		System.out.println("finished");
	}

	public static void adaptoveEikonalSolverSine() throws InterruptedException, IOException {
		BufferedWriter meshWriter = IOUtils.getWriter("floorField_ad.txt", new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/"));

		VPolygon bound = new VRectangle(-1, -1, 2, 2).toPolygon();
		VPoint q = new VPoint(0,0);
		IDistanceFunction distTarget = p -> -q.distanceToOrigin();
		ITimeCostFunction timeCostFunction = p -> {
			return 1.0 / (0.8 * Math.sin(2 * Math.PI * p.getX()) * Math.sin(2 * Math.PI * p.getY()) + 1.0);
		};

		IDistanceFunction distanceFunction = IDistanceFunction.create(bound);

		//1.0 + Math.abs(distanceFunction.apply(p)) * 0.2
		// (1) generate basic mesh
		var improver = new GenEikMesh<PVertex, PHalfEdge, PFace>(
				distanceFunction,
				p -> 0.1,
				Arrays.asList(q),
				0.1,
				GeometryUtils.boundRelative(bound.getPoints()),
				Arrays.asList(bound),
				PTriangleMeshBuilder::new);

		improver.initialize();
		for(int i = 0; i < 100; i++) {
			//Thread.sleep(50);
			improver.improve();
			//panel.repaint();
		}

		/*var boundaryEdges = improver.getTriangulation().getMesh().getBoundaryEdges();
		for(var e : boundaryEdges) {
			var edge = improver.getTriangulation().getMesh().getTwin(e);
			VPoint p1 = improver.getTriangulation().getMesh().toPoint(edge);
			VPoint p2 = improver.getTriangulation().getMesh().toPoint(improver.getTriangulation().getMesh().getNext(edge));
			VPoint p3 = improver.getTriangulation().getMesh().toPoint(improver.getTriangulation().getMesh().getPrev(edge));

			double angle1 = GeometryUtils.angle(p1, p2, p3);

			// non-acute triangle
			double rightAngle = Math.PI/2;
			if(angle1 > rightAngle + GeometryUtils.DOUBLE_EPS) {
				improver.getTriangulation().splitEdge(edge, false);
			}
		}*/

		ArrayList<IPoint> points = new ArrayList<>();
		points.add(q);

		ITriangleMeshWithDataStorage<PVertex, PHalfEdge, PFace> withDataStorage = improver.getTriangulation().getMeshBuilder().getMeshWithDataStorage();
		ITriangleMesh<PVertex, PHalfEdge, PFace> mesh = improver.getMesh();
		JumpAndWalkPointLocator<PVertex, PHalfEdge, PFace> pointLocator = new JumpAndWalkPointLocator<>(mesh);
		var solver = new MeshEikonalSolverFMM<>(
				timeCostFunction,
				points,
				//improver.getTriangulation().getMesh().getBoundaryVertices(),,
				withDataStorage,
				pointLocator);
		solver.solve();
		meshWriter.write(MeshPythonUtils.toPythonTriangulation(solver.getMeshWithDataStorage(), v -> solver.getPotential(v)));
		meshWriter.close();
		System.out.println("finished");
	}
}

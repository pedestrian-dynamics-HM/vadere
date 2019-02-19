package org.vadere.simulator.models.potential.solver;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.PEikMeshGen;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.simulator.models.potential.solver.calculators.mesh.PotentialPoint;
import org.vadere.simulator.models.potential.solver.calculators.mesh.EikonalSolverFMMTriangulation;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;

import java.util.List;
import java.util.stream.Collectors;


public class PerformanceTriangleFMM {

	private static Logger log = LogManager.getLogger(TestFFMNonUniformTriangulation.class);
	private static final VRectangle bbox = new VRectangle(-12, -12, 24, 24);
	private static final IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
	private static final double initialEdgeLen = 0.6;

	static {
		//LogManager.shutdown();
	}

	private static IIncrementalTriangulation<PotentialPoint, Double, Object, PVertex<PotentialPoint, Double, Object>, PHalfEdge<PotentialPoint, Double, Object>, PFace<PotentialPoint,Double, Object>> createTriangulation() {
		IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
		PEikMeshGen<PotentialPoint, Double, Object> meshGenerator = new PEikMeshGen<>(distanceFunc, edgeLengthFunc, initialEdgeLen, bbox, (x, y) -> new PotentialPoint(x, y));
		return meshGenerator.generate();
	}

	private static void solve(EikonalSolverFMMTriangulation<PotentialPoint, PVertex<PotentialPoint, Double, Object>, PHalfEdge<PotentialPoint, Double, Object>, PFace<PotentialPoint, Double, Object>> solver) {
		long ms = System.currentTimeMillis();
		System.out.println("start FFM");
		solver.initialize();
		System.out.println("FFM finished");
		System.out.println("time: " + (System.currentTimeMillis() - ms));
	}


	public static void main(String... args) {
		/**
		 * (1) create mesh
		 */
		IIncrementalTriangulation<PotentialPoint, Double, Object, PVertex<PotentialPoint, Double, Object>, PHalfEdge<PotentialPoint, Double, Object>, PFace<PotentialPoint, Double, Object>> triangulation = createTriangulation();

		/**
		 * (2) define target points
		 */
		/**
		 * (2) define target points
		 */
		List<PVertex<PotentialPoint, Double, Object>> targetVertices = triangulation.getMesh().getBoundaryVertices().stream().collect(Collectors.toList());

		/**
		 * (3) solve the eikonal equation on the mesh
		 */
		EikonalSolverFMMTriangulation<PotentialPoint, PVertex<PotentialPoint, Double, Object>, PHalfEdge<PotentialPoint, Double, Object>, PFace<PotentialPoint, Double, Object>> solver = new EikonalSolverFMMTriangulation(
				new UnitTimeCostFunction(),
				triangulation,
				targetVertices,
				distanceFunc);

		/**
		 * (3) solve the eikonal equation on the mesh
		 */
		while (true) {
			/*try {
				new MeshPanel<>(triangulation.getMesh(), 900, 900, bbox).display();
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
			solve(solver);
			System.out.println("nPoints: " + (triangulation.getMesh().getNumberOfVertices()));
		}
	}
}

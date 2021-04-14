package org.vadere.simulator.models.potential.solver;

import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;

import java.util.List;
import java.util.stream.Collectors;


public class PerformanceTriangleFMM {

	private static Logger log = Logger.getLogger(TestFFMNonUniformTriangulation.class);
	private static final VRectangle bbox = new VRectangle(-12, -12, 24, 24);
	private static final IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
	private static final double initialEdgeLen = 0.6;

	static {
		//LogManager.shutdown();
	}

	private static IIncrementalTriangulation<PVertex, PHalfEdge, PFace> createTriangulation() {
		IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
		PEikMesh meshGenerator = new PEikMesh(distanceFunc, edgeLengthFunc, initialEdgeLen, bbox);
		return meshGenerator.generate();
	}

	private static void solve(MeshEikonalSolverFMM<PVertex, PHalfEdge, PFace> solver) {
		long ms = System.currentTimeMillis();
		System.out.println("start FFM");
		solver.solve();
		System.out.println("FFM finished");
		System.out.println("time: " + (System.currentTimeMillis() - ms));
	}


	public static void main(String... args) {
		/**
		 * (1) create mesh
		 */
		IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation = createTriangulation();

		/**
		 * (2) define target points
		 */
		/**
		 * (2) define target points
		 */
		List<PVertex> targetVertices = triangulation.getMesh().getBoundaryVertices().stream().collect(Collectors.toList());

		/**
		 * (3) solve the eikonal equation on the mesh
		 */
		MeshEikonalSolverFMM<PVertex, PHalfEdge, PFace> solver = new MeshEikonalSolverFMM(
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

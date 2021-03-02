package org.vadere.simulator.examples.Meshing;

import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.utils.io.IOUtils;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.IDistanceFunction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Code to generate the circle eikonal example of the PhD thesis of B. Zoennchen (p. 184, Fig 9.6).
 *
 * @author Benedikt Zoennchen
 */
public class CirclesFMM {

	public static void main(String ... args) throws IOException, InterruptedException {
		circleEikMeshAndFMM();
	}

	public static void circleEikMeshAndFMM() throws InterruptedException, IOException {

		BufferedWriter meshWriter = IOUtils.getWriter("floorFields.txt", new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/"));

		VRectangle domain = new VRectangle(-1,-1,2,2);
		IDistanceFunction distanceFunction = IDistanceFunction.create(domain);
		ArrayList<IPoint> points = new ArrayList<>();
		VPoint point = new VPoint(0,0);
		points.add(point);

		// (1) generate basic mesh
		double h0 = 0.025;
		var improver = new GenEikMesh<>(
				distanceFunction,
				p -> h0 + 0.3 * point.distanceSq(p),
				points,
				h0,
				GeometryUtils.boundRelative(domain.toPolygon().getPoints()),
				Arrays.asList(domain),
				() -> new PMesh());

		improver.initialize();
		MeshPanel<PVertex, PHalfEdge, PFace> panel = new MeshPanel<PVertex, PHalfEdge, PFace>(improver.getMesh(), 1000, 1000);
		panel.display();
		for(int i = 0; i < 200; i++) {
			Thread.sleep(50);
			improver.improve();
			panel.repaint();
		}


		var solver = new MeshEikonalSolverFMM<>(
				"",
				new UnitTimeCostFunction(),
				points,
				improver.getTriangulation());
		solver.solve();
		meshWriter.write(improver.getTriangulation().getMesh().toPythonTriangulation(v -> solver.getPotential(v)));
		meshWriter.close();
		System.out.println("finished");
		System.out.println(improver.getTriangulation().getMesh().toPythonTriangulation(v -> solver.getPotential(v)));

	}
}

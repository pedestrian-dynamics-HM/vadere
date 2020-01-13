package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.vadere.meshing.examples.MeshExamples;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public class FMMTriangulationExamples {

	private static Logger log = Logger.getLogger(FMMTriangulationExamples.class);

	public static void main(String... args) throws IOException, InterruptedException {
		var triangulation = bridge();
		VPolygon targetShape = GeometryUtils.toPolygon(new VPoint(33.90000000002328, 0.20000000018626451),
				new VPoint(29.900000000023283, 0.5),
				new VPoint(32.300000000046566, 6.0),
				new VPoint(36.40000000002328, 4.900000000372529));

		EikonalSolver solver = new EikonalSolverFMMTriangulation(Arrays.asList(targetShape), new UnitTimeCostFunction(), triangulation);
		log.info("start FFM");
		solver.initialize();
		log.info("FFM finished");
		System.out.println(triangulation.getMesh().toPythonTriangulation(p -> triangulation.getMesh().getDoubleData(p, EikonalSolverFMMTriangulation.namePotential)));
	}

	public static IIncrementalTriangulation<
			PVertex,
				PHalfEdge,
				PFace> bridge() throws IOException, InterruptedException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/bridge.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		Collection<VPolygon> holes = pslg.getHoles();
		VPolygon segmentBound = pslg.getSegmentBound();

		IPointConstructor<PotentialPoint> pointConstructor = (x, y) -> new PotentialPoint(x, y);
		IDistanceFunction distanceFunction = IDistanceFunction.create(segmentBound, holes);


		// (3) use EikMesh to improve the mesh
		double h0 = 1.0;
		var meshImprover = new PEikMesh(
				distanceFunction,
				p -> h0 + 0.5 * Math.abs(distanceFunction.apply(p)),
				h0,
				new VRectangle(segmentBound.getBounds2D()),
				pslg.getHoles()
		);

		var mesh = meshImprover.getMesh();
		Color green = new Color(85, 168, 104);
		Color red = new Color(196,78,82);
		Color blue = new Color(76,114,202);
		Function<PFace, Color> colorFunction = f -> {
			VPoint midpoint = mesh.toTriangle(f).midPoint();
			if(midpoint.getY() < 46  && midpoint.getX() < 10) {
				return blue;
			}
			else if(midpoint.getY() < 20) {
				return green;
			} else {
				return red;
			}
		};

		var meshPanel = new PMeshPanel(meshImprover.getMesh(), 1000, 800, colorFunction);
		meshPanel.display("Combined distance functions " + h0);
		while (!meshImprover.isFinished()) {
			meshImprover.improve();
			Thread.sleep(20);
			meshPanel.repaint();
		}
		//meshImprover.generate();
		// display the mesh

		meshPanel.display("Combined distance functions " + h0);
		System.out.println(TexGraphGenerator.toTikz(mesh, colorFunction, null, 1.0f, true));
		return meshImprover.getTriangulation();
	}
}

package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.utils.math.GeometryUtilsMesh;

import java.util.function.Predicate;

public class PredicateEdgeRefineCurvature<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Predicate<V> {

	private MeshEikonalSolverFMMRefined<V, E, F> solver;
	private double maxCurvature;

	public PredicateEdgeRefineCurvature(@NotNull final MeshEikonalSolverFMMRefined<V, E, F> solver, final double maxCurvature) {
		this.maxCurvature = maxCurvature;
		this.solver = solver;
	}

	@Override
	public boolean test(V v) {
		double[] result = GeometryUtilsMesh.curvature(solver.getMesh(), v, vertex -> solver.getPotential(vertex));
		return result[0] > maxCurvature;
	}

	/*@Override
	public boolean test(E e) {
		VLine line = getMesh().toLine(e);
		double len = line.length();
		double x[] = new double[3];
		double y[] = new double[3];
		double z[] = new double[3];

		VPoint p = line.midPoint();
		triangulation.getTriPoints(getMesh().getFace(e), x, y, z, v -> getMesh().getCurvature(v));
		double totalArea = GeometryUtils.areaOfPolygon(x, y);
		double curvature = InterpolationUtil.barycentricInterpolation(x, y, z, totalArea, p.getX(), p.getY());
		return len > minEdgeLen && curvature > maxCurvature;
	}*/
}

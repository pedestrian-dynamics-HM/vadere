package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.utils.math.GeometryUtilsMesh;
import org.vadere.util.geometry.shapes.VLine;
import java.util.function.Predicate;

public class PredicateEdgeRefinement<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Predicate<E> {
	private final MeshEikonalSolverFMM<V, E, F> solver;
	private double minEdgeLen;
	private double maxCurvature;

	public PredicateEdgeRefinement(
			@NotNull final MeshEikonalSolverFMM<V, E, F> solver,
			final double minEdgeLen,
			final double maxCurvature
	){
		this.solver = solver;
		this.minEdgeLen = minEdgeLen;
		this.maxCurvature = maxCurvature;
	}

	@Override
	public boolean test(E e) {
		V vertex = solver.getMesh().getVertex(e);
		V vertexTwin = solver.getMesh().getTwinVertex(e);
		VLine line = solver.getMesh().toLine(e);
		double len = line.length();
		double maxCurvature = 0.0;

		if(solver.isBurned(vertex) && solver.isBurned(vertexTwin)) {
			for (E edge : solver.getMesh().getEdgeIt(vertex)) {
				double[] curvatures = GeometryUtilsMesh.curvature(solver.getMesh(), v -> solver.getPotential(v), v -> solver.isBurned(v), edge);
				maxCurvature = Math.max(maxCurvature, curvatures[1]);
			}

			for (E edge : solver.getMesh().getEdgeIt(vertexTwin)) {
				double[] curvatures = GeometryUtilsMesh.curvature(solver.getMesh(), v -> solver.getPotential(v), v -> solver.isBurned(v), edge);
				maxCurvature = Math.max(maxCurvature, curvatures[1]);
			}
		}
		return len > minEdgeLen && maxCurvature > this.maxCurvature;
	}
}

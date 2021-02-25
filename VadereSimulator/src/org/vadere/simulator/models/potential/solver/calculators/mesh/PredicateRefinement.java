package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.utils.math.GeometryUtilsMesh;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.InterpolationUtil;

import java.util.function.Predicate;

public class PredicateRefinement<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Predicate<E> {

	private final IIncrementalTriangulation<V, E, F> backgroundMesh;
	private final IIncrementalTriangulation<V, E, F> refineMesh;
	private double minEdgeLen;
	private double delta;

	public PredicateRefinement(
			@NotNull final IIncrementalTriangulation<V, E, F> backgroundMesh,
			@NotNull final IIncrementalTriangulation<V, E, F> refineMesh,
			final double minEdgeLen,
			final double delta
	){
		this.backgroundMesh = backgroundMesh;
		this.refineMesh = refineMesh;
		this.minEdgeLen = minEdgeLen;
		this.delta = delta;
	}

	@Override
	public boolean test(E e) {
		//GeometryUtilsMesh.curvature(backgroundMesh.getMesh(), )
		VLine line = refineMesh.getMesh().toLine(e);
		double len = line.length();
		VPoint p = line.midPoint();
		double curvature = backgroundMesh.getInterpolatedValue(p.getX(), p.getY(), MeshEikonalSolverFMMIterative.nameCurvature);
		if(curvature <= GeometryUtils.DOUBLE_EPS) {
			return false;
		}
		return len > minEdgeLen + minEdgeLen * delta * (1/(curvature));
	}
}

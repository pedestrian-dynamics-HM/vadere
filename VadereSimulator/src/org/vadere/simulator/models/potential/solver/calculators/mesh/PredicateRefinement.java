package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.function.Predicate;

/**
 * @author Benedikt Zonnchen
 *
 * This predicate decides if a mesh should be further refined.
 * It compares the curvature of the travel time of the propagating wave front
 * of the previous iteration. Compare PhD thesis B. Zoennchen Section 9.4.3.
 */
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

		// this formula can be found in the PhD thesis of B. Zoennchen, page 186 Eq. 9.44.
		return len > minEdgeLen + minEdgeLen * delta * (1/(curvature));
	}
}

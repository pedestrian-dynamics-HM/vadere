package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.InterpolationUtil;

import java.util.function.Predicate;

public class PredicateRefinement<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Predicate<E> {

	private final IIncrementalTriangulation<V, E, F> backgroundMesh;
	private final IIncrementalTriangulation<V, E, F> refineMesh;
	private double minEdgeLen;
	private double maxCurvature;

	public PredicateRefinement(
			@NotNull final IIncrementalTriangulation<V, E, F> backgroundMesh,
			@NotNull final IIncrementalTriangulation<V, E, F> refineMesh,
			final double minEdgeLen,
			final double maxCurvature
	){
		this.backgroundMesh = backgroundMesh;
		this.refineMesh = refineMesh;
		this.minEdgeLen = minEdgeLen;
		this.maxCurvature = maxCurvature;
	}

	@Override
	public boolean test(E e) {
		VLine line = refineMesh.getMesh().toLine(e);
		double len = line.length();
		double x[] = new double[3];
		double y[] = new double[3];
		double z[] = new double[3];

		VPoint p = line.midPoint();
		var face = backgroundMesh.locateFace(p).get();
		backgroundMesh.getTriPoints(face, x, y, z, MeshEikonalSolverFMMIterative.nameCurvature);
		double totalArea = GeometryUtils.areaOfPolygon(x, y);
		double curvature = InterpolationUtil.barycentricInterpolation(x, y, z, totalArea, p.getX(), p.getY());
		return len > minEdgeLen && curvature > maxCurvature;
	}
}

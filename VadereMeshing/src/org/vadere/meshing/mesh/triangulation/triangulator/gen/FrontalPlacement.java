package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IPlacementStrategy;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.function.Function;

public class FrontalPlacement<P extends IPoint, CE, CF, V extends IVertex, E extends IHalfEdge, F extends IFace> implements IPlacementStrategy<V, E ,F> {
	private IMesh<V, E, F> mesh;
	private DelaunayPlacement<V, E, F> delaunayPlacement;
	private VoronoiSegPlacement<V, E, F> voronoiSegPlacement;
	private VoronoiSegSizeOptimalPlacement<P, CE, CF, V, E, F> voronoiSegSizeOptimalPlacement;

	public FrontalPlacement(@NotNull final IMesh<V, E, F> mesh,
	                        @NotNull final Function<IPoint, Double> circumRadiusFunc,
	                        final double qmin) {
		this.mesh = mesh;
		this.delaunayPlacement = new DelaunayPlacement<>(mesh);
		this.voronoiSegPlacement = new VoronoiSegPlacement<>(mesh, circumRadiusFunc);
		this.voronoiSegSizeOptimalPlacement = new VoronoiSegSizeOptimalPlacement<>(mesh, qmin);
	}

	@Override
	public IMesh<V, E, F> getMesh() {
		return mesh;
	}

	@Override
	public VPoint computePlacement(@NotNull final E edge, final VTriangle triangle) {
		VLine line = getMesh().toLine(edge);
		VPoint midpoint = line.midPoint();
		VPoint x1 = delaunayPlacement.computePlacement(edge, triangle);
		VPoint x2 = voronoiSegPlacement.computePlacement(edge, triangle);
		VPoint x3 = voronoiSegSizeOptimalPlacement.computePlacement(edge, triangle);


		double d1 = x1.distance(midpoint);
		double d2 = x2.distance(midpoint);
		double d3 = x3.distance(midpoint);

		if(d2 <= d1 && d2 <= d3 && d2 >= 0.5 * line.length()) {
			return x2;
		} else if(d3 <= d1) {
			return x3;
		} else {
			return x1;
		}
	}
}

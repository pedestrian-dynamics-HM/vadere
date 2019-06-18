package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IPlacementStrategy;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

public class VoronoiSegSizeOptimalPlacement<P extends IPoint, CE, CF, V extends IVertex, E extends IHalfEdge, F extends IFace> implements IPlacementStrategy<V, E ,F> {
	private IMesh<V, E, F> mesh;
	private double qmin;

	public VoronoiSegSizeOptimalPlacement(
			@NotNull final IMesh<V, E, F> mesh,
			final double qmin) {
		this.mesh = mesh;
		this.qmin = Math.toRadians(qmin);
	}

	@Override
	public IMesh<V, E, F> getMesh() {
		return mesh;
	}

	@Override
	public VPoint computePlacement(@NotNull final E edge) {
		return computePlacement(edge, null);
	}

	@Override
	public VPoint computePlacement(@NotNull final E edge, @Nullable final VTriangle triangle) {
		VLine line = getMesh().toLine(edge);
		VPoint midpoint = line.midPoint();
		VPoint c = triangle.getCircumcenter();
		double d = 0.5 * line.length() / Math.tan(0.5 * qmin);

		VPoint e;
		VPoint x;
		VPoint cc;
		if(!getMesh().isAtBoundary(edge)) {
			cc = getMesh().toTriangle(getMesh().getTwinFace(edge)).getCircumcenter();
		} else {
			double incircleRadius = Math.sqrt(3) / 6.0 * line.length();
			VPoint dir = line.asVPoint().rotate(Math.PI * 0.5).setMagnitude(incircleRadius);
			cc = midpoint.add(dir);
		}

		e = c.subtract(cc).norm();
		x = midpoint.add(e.scalarMultiply(d));
		return x;
	}

}

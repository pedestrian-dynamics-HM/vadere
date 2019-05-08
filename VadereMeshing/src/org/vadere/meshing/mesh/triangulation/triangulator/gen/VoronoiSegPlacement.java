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

import java.util.function.Function;

/**
 * Computes insertion points based on a Frontal-Delaunay strategy, that is
 * the point lies on the Voronoi segment of the corresponding Voronoi-diagram.
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class VoronoiSegPlacement<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements IPlacementStrategy<P, CE, CF, V, E ,F> {
	private IMesh<P, CE, CF, V, E, F> mesh;
	private Function<IPoint, Double> circumRadiusFunc;

	public VoronoiSegPlacement(
			@NotNull final IMesh<P, CE, CF, V, E, F> mesh,
			@NotNull final Function<IPoint, Double> circumRadiusFunc) {
		this.mesh = mesh;
		this.circumRadiusFunc = circumRadiusFunc;
	}

	@Override
	public IMesh<P, CE, CF, V, E, F> getMesh() {
		return mesh;
	}

	@Override
	public VPoint computePlacement(@NotNull final E edge) {
		return computePlacement(edge, null);
	}

	@Override
	public VPoint computePlacement(@NotNull final E edge, @Nullable final VTriangle triangle) {
		F face = getMesh().getFace(edge);
		E shortestEdge = edge;
		VLine line = getMesh().toLine(edge);

		VPoint midpoint = line.midPoint();
		VPoint c = triangle.getCircumcenter();
		double pq = 0.5 * line.length();
		//double pc = new VLine(c, new VPoint(getMesh().getPoint(shortestEdge))).length();

		double r = circumRadiusFunc.apply(midpoint);
		/*if(getMesh().isAtBoundary(shortestEdge)) {
			double s = ((2 * pq) / Math.sqrt(3));
			if(s / circumRadiusFunc.apply(getMesh().toTriangle(face).midPoint()) < maxRadiusRatio) {
				r = s;
			}
		}*/

		double mc = new VLine(c, midpoint).length();
		double rmax = (pq * pq + mc * mc) / (2 * mc);
		r = Math.min(Math.max(r, pq), rmax);
		double d = Math.sqrt(r * r - pq * pq) + r;
		/*VPoint e;
		VPoint x;
		if(!getMesh().isBoundary(getMesh().getTwinFace(shortestEdge))) {
			VPoint cc = getMesh().toTriangle(getMesh().getTwinFace(shortestEdge)).getCircumcenter();
			e = c.subtract(cc).norm();
			x = midpoint.add(e.scalarMultiply(d));
		} else {
			if(c.distanceSq(midpoint) < GeometryUtils.DOUBLE_EPS) {
				x = midpoint;
			} else {
				// would otherwise result in a very large angle at the boundary
				if(d / Math.sqrt((3*pq * pq)) < 0.8) {
					x = midpoint;
				}
				else {
					e = c.subtract(midpoint).norm();
					x = midpoint.add(e.scalarMultiply(d));
				}
			}
		}*/

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

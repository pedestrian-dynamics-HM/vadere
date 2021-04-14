package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 *
 */
public interface MeshEikonalSolver<V extends IVertex, E extends IHalfEdge, F extends IFace> extends EikonalSolver {

	/**
	 * Types of gradient approximations.
	 */
	enum LocalSover {
		SETHIAN, MATRIX;
	}

	double getPotential(@NotNull final V v);

	IMesh<V, E, F> getMesh();

	boolean isFeasibleForComputation(@NotNull final V v);

	double getTimeCost(@NotNull final V v);

	ITimeCostFunction getTimeCostFunction();

	@Override
	default void update() {
		if (needsUpdate()) {
			getTimeCostFunction().update();
			solve();
			//System.out.println(triangulation.getMesh().toPythonTriangulation(v -> getPotential(v)));
			//System.out.println();
		}
	}

	default double computePotential(@NotNull final V point, V point1, V point2, double cosPhi, @NotNull final LocalSover localSover) {

		if(getPotential(point1) > getPotential(point2)) {
			V tmp = point1;
			point1 = point2;
			point2 = tmp;
		}

		VPoint p = getMesh().toPoint(point);
		VPoint p1 = getMesh().toPoint(point1);
		VPoint p2 = getMesh().toPoint(point2);

		boolean bVert1Usable = isFeasibleForComputation(point1);
		boolean bVert2Usable = isFeasibleForComputation(point2);

		double F_inverse = getTimeCost(point);

		if(bVert1Usable || bVert2Usable) {
			VPoint edge1 = p1.subtract(p);
			VPoint edge2 = p2.subtract(p);

			// TODO save this in the mesh?
			double b = GeometryUtils.length(edge1.getX(), edge1.getY());
			double a = GeometryUtils.length(edge2.getX(), edge2.getY());

			//edge1 = edge1.scalarMultiply(1.0 / b);
			//edge2 = edge2.scalarMultiply(1.0 / a);

			double d1 = getPotential(point1);
			double d2 = getPotential(point2);
			double dot = cosPhi < 0 ? computeCosPhi(point1, point, point2) : cosPhi;

			if( !bVert1Usable && bVert2Usable )
			{
				/* only one point is a contributor */
				return d2 + a * F_inverse;
				//return Double.MAX_VALUE;
			}
			if( bVert1Usable && !bVert2Usable )
			{
				/* only one point is a contributor */
				return d1 + b * F_inverse;
				//return Double.MAX_VALUE;
			}

			switch (localSover){
				case MATRIX: return computePotentialMatrix(d1, d2, a, b, dot, F_inverse);
				case SETHIAN:
				default: return computePotentialSethian(d1, d2, a, b, dot, F_inverse);
			}
		}
		else {
			return Double.MAX_VALUE;
		}
	}

	default double computeCosPhi(@NotNull final V p1, @NotNull final V p, @NotNull final V p2) {
		return Math.cos(GeometryUtils.angle(getMesh().getX(p1), getMesh().getY(p1), getMesh().getX(p), getMesh().getY(p), getMesh().getX(p2), getMesh().getY(p2)));
	}

	/**
	 * Similar to https://github.com/gpeyre/matlab-toolboxes/blob/master/toolbox_fast_marching/mex/gw/gw_geodesic/GW_GeodesicMesh.inl
	 * method ComputeUpdate_MatrixMethod
	 * @param d1
	 * @param d2
	 * @param a
	 * @param b
	 * @param dot
	 * @param F_inverse
	 * @return
	 */
	default double computePotentialMatrix(
			final double d1, final double d2,
			final double a, final double b,
			final double dot, final double F_inverse) {
		double t;

		/* the directional derivative is D-t*L */
		double[] D = new double[]{ d1/b, d2/a };
		double[] L = new double[]{ 1/b,  1/a };

		double[] QL;	//Q*L
		double[] QD;	//Q*L

		double det = 1-dot*dot;		// 1/det(Q) where Q=(P*P^T)^-1

		QD = new double[]{1/det * (D[0] - dot*D[1] ), 1/det * (- dot*D[0] + D[1])};
		QL = new double[]{1/det * (L[0] - dot*L[1] ), 1/det * (- dot*L[0] + L[1])};


		/* compute the equation 'e2*t² + 2*e1*t + e0 = 0' */
		double e2 = QL[0]*L[0] + QL[1]*L[1];			// <L,Q*L>
		double e1 = -( QD[0]*L[0] + QD[1]*L[1] );		// -<L,Q*D>
		double e0 = QD[0]*D[0] + QD[1]*D[1] - F_inverse*F_inverse;	// <D,Q*D> - F²

		double delta = e1*e1 - e0*e2;

		if(delta >= 0)
		{
			if( Math.abs(e2) > GeometryUtils.DOUBLE_EPS)
			{
				/* there is a solution */
				t = (-e1 - Math.sqrt(delta) )/e2;
				/* upwind criterion : Q*(D-t*l)<=0, i.e. QD<=t*QL */
				if( t<Math.max(d1,d2) || QD[0]>t*QL[0] || QD[1]>t*QL[1] )
					t = (-e1 + Math.sqrt(delta) )/e2;	// criterion not respected: choose bigger root.
			}
			else
			{
				if( e1!=0 )
					t = -e0/e1;
				else
					t = -Double.MAX_VALUE;
			}
		}
		else
			t = -Double.MAX_VALUE;
		/* choose the update from the 2 vertex only if upwind criterion is met */
		if( t>=Math.max(d1,d2) && QD[0]<=t*QL[0] && QD[1]<=t*QL[1] )
			return t;
		else
			return Math.min(b*F_inverse+d1,a*F_inverse+d2);
	}

	/**
	 * Similar to https://github.com/gpeyre/matlab-toolboxes/blob/master/toolbox_fast_marching/mex/gw/gw_geodesic/GW_GeodesicMesh.inl
	 * method ComputeUpdate_SethianMethod.
	 *
	 * @param d1
	 * @param d2
	 * @param a
	 * @param b
	 * @param dot
	 * @param F_inverse
	 * @return
	 */
	default double computePotentialSethian(
			final double d1, final double d2,
			final double a, final double b,
			final double dot, final double F_inverse) {
		double t = Double.MAX_VALUE;
		double rCosAngle = dot;
		double rSinAngle = Math.sqrt(1 - dot * dot);

		/* Sethian method */
		double u = d2-d1;		// T(B)-T(A)
		assert u >= 0;
		double f2 = a*a+b*b-2*a*b*rCosAngle;
		double f1 = b*u*(a*rCosAngle-b);
		double f0 = b*b*(u*u-F_inverse*F_inverse*a*a*rSinAngle*rSinAngle);

		/* discriminant of the quartic equation */
		double delta = f1*f1 - f0*f2;

		if(delta >= 0)
		{
			if(Math.abs(f2) > GeometryUtils.DOUBLE_EPS)
			{
				/* there is a solution */
				t = (-f1 - Math.sqrt(delta) )/f2;
				/* test if we must must choose the other solution */
				if( t<u ||
						b*(t-u)/t < a*rCosAngle ||
						a/rCosAngle < b*(t-u)/t )
				{
					t = (-f1 + Math.sqrt(delta) )/f2;
				}
			}
			else
			{
				/* this is a 1st degree polynom */
				if( f1!=0 )
					t = - f0/f1;
				else
					t = Double.MAX_VALUE;
			}
		}
		else
			t = -Double.MAX_VALUE;

		/* choose the update from the 2 vertex only if upwind criterion is met */
		if( u<t &&
				a*rCosAngle < b*(t-u)/t &&
				b*(t-u)/t < a/rCosAngle )
		{
			return t+d1;
		}
		else
		{
			return Math.min(b*F_inverse+d1,a*F_inverse+d2);
		}
	}
}

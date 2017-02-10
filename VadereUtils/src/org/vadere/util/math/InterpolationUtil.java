package org.vadere.util.math;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.data.Face;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.calculators.PotentialPoint;

import java.util.List;


/**
 * Interpolation utilities not covered by java.lang.Math
 * 
 */
public class InterpolationUtil {

	public static double barycentricInterpolation(final Face<? extends PotentialPoint> triangle, final double x, final double y){
		List<? extends PotentialPoint> points = triangle.getPoints();
		assert points.size() == 3;

		PotentialPoint p1 = points.get(0);
		PotentialPoint p2 = points.get(1);
		PotentialPoint p3 = points.get(2);
		VTriangle vtriangle = triangle.toTriangle();

		double totalArea = vtriangle.getArea();

		assert totalArea > 0;

		VPoint point = new VPoint(x, y);

		double value = 0.0;

		if(point.equals(p1)) {
			value = p1.getPotential();
		}
		else if(point.equals(p2)) {
			value = p2.getPotential();
		}
		else if(point.equals(p3)) {
			value = p3.getPotential();
		}
		else {
			double area1 = new VTriangle(new VPoint(p2), new VPoint(p3), point).getArea();
			double area2 = new VTriangle(new VPoint(p1), new VPoint(p3), point).getArea();
			double area3 = new VTriangle(new VPoint(p1), new VPoint(p2), point).getArea();

			if(area1 > 0.0) {
				double percentP1 = area1 / totalArea;
				value += percentP1 * p1.getPotential();
			}

			if(area2 > 0.0) {
				double percentP2 = area2 / totalArea;
				value += percentP2 * p2.getPotential();
			}


			if(area3 > 0.0) {
				double percentP3 = area3 / totalArea;
				value += percentP3 * p3.getPotential();
			}
		}
		return value;
	}

	/**
	 * Computes bilinear interpolation of z = f(x,y) with z1 to z4 being the
	 * given z values at the edges of a rectangle in the xy-plane. z1 refers to
	 * the left-lower edge of the rectangle. The remaining z values have to be
	 * assigned counter-clockwise to the rectangle edges. t and u are the
	 * normalized offsets of the left-lower edge of the rectangle along the x-
	 * and y-axis.
	 */
	public static double bilinearInterpolation(double z1, double z2, double z3,
			double z4, double t, double u) {
		double result = 0;
		double value = 0;
		double weight = 0;

		for (int i = 0; i < 4; ++i) {
			switch (i) {
				case 0:
					value = z1;
					weight = (1 - t) * (1 - u);
					break;
				case 1:
					value = z2;
					weight = t * (1 - u);
					break;
				case 2:
					value = z3;
					weight = t * u;
					break;
				case 3:
					value = z4;
					weight = (1 - t) * u;
					break;
			}

			result += weight * value;
		}

		return result;
	}

    /**
     * Computes bilinear interpolation for (x,y) while nodes may be undefined
     * (=Double.MAX_VALUE).
     *
     * See bilinearInterpolation for further information
     * about the basic method. In contrast to the original
     * bilinearInterpolation(), bilinearInterpolationWithUnkown() just
     * accumulates the nodes whose values are known multiplied by its weight.
     * The accumulated weight of the known values are stored in the parameter
     * weightOfKnown. see https://en.wikipedia.org/wiki/Bilinear_interpolation (Unit square)
     *
     * @param z             the values of the 4 grid points which convex hull contains (x,y) in the following order:
     *                      (0,0), (1,0), (0,1), (1,1) with respect to (x,y)
     * @param dx            x - x1 where (x1,y1) is the point at the left below corner of the four points
     * @param dy            y - y1 where (x1,y1) is the point at the left below corner of the four points
     * @return a pair (bilinear interpolated value, accumulated weights of known)
     */
	public static Pair<Double, Double> bilinearInterpolationWithUnkown(double z[], double dx, double dy) {
		double result = 0;
		double knownWeights = 0;
		double weight[] = {(1 - dx) * (1 - dy), dx * (1 - dy), dx * dy, (1 - dx) * dy};

		for (int i = 0; i < 4; ++i) {
			if (z[i] != Double.MAX_VALUE) {
				result += weight[i] * z[i];
                knownWeights += weight[i];
			}
		}

		return Pair.of(result, knownWeights);
	}


	/**
	 * Get the potential value based on a triangulation of the grid.
	 * 
	 * @param pot
	 * @param x
	 * @param y
	 * @return
	 */
	public static double getValueByTriangleInterpolation(CellGrid pot,
			double x, double y) {
		double[] cross = new double[3]; // a temp variable for cross products

		// clamp to grid
		x = Math.max(0, Math.min(pot.getNumPointsX() - 2, x)); // the start 1 is s.t. we
		// can add one later
		y = Math.max(0, Math.min(pot.getNumPointsY() - 2, y)); // the start 1 is s.t. we
		// can add one later

		// get versions that are local in between grid points
		double locX = x - (int) x;
		double locY = y - (int) y;

		// get coordinates of four grid points,
		// lying to the right and bottom of the current point x1,y1
		int x1 = (int) x;
		int x2 = (int) x;
		int x3 = (int) x + 1;
		int x4 = (int) x + 1;
		int y1 = (int) y;
		int y2 = (int) y + 1;
		int y3 = (int) y + 1;
		int y4 = (int) y;

		// compute the plane spanned by v1 and v2
		double z1 = pot.getValue(x1, y1).potential;
		double z31 = pot.getValue(x3, y3).potential - z1;
		double[] v1 = new double[3];
		double[] v2 = new double[] {x3 - x1, y3 - y1, z31};

		// check whether its the upper or lower triangle
		if (locX < locY) // upper triangle
		{
			double z21 = pot.getValue(x2, y2).potential - z1;

			v1[0] = x2 - x1;
			v1[1] = y2 - y1;
			v1[2] = z21;
		} else // lower triangle
		{
			double z41 = pot.getValue(x4, y4).potential - z1;

			v1[0] = x4 - x1;
			v1[1] = y4 - y1;
			v1[2] = z41;
		}

		MathUtil.cross(v1, v2, cross); // normal vector

		double k = cross[0] * 0 + cross[1] * 0 + cross[2] * z1;

		// return pot.potential[y1][x1];
		return 1 / cross[2] * (k - cross[0] * locX - cross[1] * locY);
	}

	/**
	 * Gauss quadrature x values for the interval [-1,1] and N=11 steps.
	 * 
	 * @see http
	 *      ://processingjs.nihongoresources.com/bezierinfo/legendre-gauss-values
	 *      .php
	 */
	private static double[] GaussQuadraturePoints = new double[] {
			-0.993752171, -0.967226839, -0.920099334, -0.853363365,
			-0.768439963, -0.667138804, -0.551618836, -0.42434212,
			-0.288021317, -0.145561854, 0, 0.145561854, 0.288021317,
			0.42434212, 0.551618836, 0.667138804, 0.768439963, 0.853363365,
			0.920099334, 0.967226839, 0.993752171,};
	private static double[] NormalQuadraturePoints = new double[] {
			-1.000000000000000, -0.933333333333333, -0.866666666666667,
			-0.800000000000000, -0.733333333333333, -0.666666666666667,
			-0.600000000000000, -0.533333333333333, -0.466666666666667,
			-0.400000000000000, -0.333333333333333, -0.266666666666667,
			-0.200000000000000, -0.133333333333333, -0.066666666666667, 0,
			0.066666666666667, 0.133333333333333, 0.200000000000000,
			0.266666666666667, 0.333333333333333, 0.400000000000000,
			0.466666666666667, 0.533333333333333, 0.600000000000000,
			0.666666666666667, 0.733333333333333, 0.800000000000000,
			0.866666666666667, 0.933333333333333, 1.000000000000000};
	private static double[] ExponentialQuadraturePoints = new double[] {
			-1.0000, -0.8333, -0.6667, -0.5000, -0.3333, -0.1667, 0, 0.1667,
			0.3333, 0.5000, 0.6667, 0.8333, 1.0000};
	/**
	 * Gauss quadrature weights for the interval [-1,1] and N=11 steps.
	 * 
	 * @see http
	 *      ://processingjs.nihongoresources.com/bezierinfo/legendre-gauss-values
	 *      .php
	 */
	private static double[] GaussQuadratureWeights = new double[] {
			0.016017228, 0.03695379, 0.057134425, 0.076100114, 0.093444423,
			0.108797299, 0.121831416, 0.132268939, 0.139887395, 0.144524404,
			0.146081134, 0.144524404, 0.139887395, 0.132268939, 0.121831416,
			0.108797299, 0.093444423, 0.076100114, 0.057134425, 0.03695379,
			0.016017228};
	private static double[] ExponentialQuadratureWeights = new double[] {
			0.0419, 0.3319, -0.3449, 1.4980, -2.4841, 4.1669, -4.4195, 4.1669,
			-2.4841, 1.4980, -0.3449, 0.3319, 0.0419};

	private static double[] NormalQuadratureWeights = new double[] {
			0.032258064516129, 0.032258064516129, 0.032258064516129,
			0.032258064516129, 0.032258064516129, 0.032258064516129,
			0.032258064516129, 0.032258064516129, 0.032258064516129,
			0.032258064516129, 0.032258064516129, 0.032258064516129,
			0.032258064516129, 0.032258064516129, 0.032258064516129,
			0.032258064516129, 0.032258064516129, 0.032258064516129,
			0.032258064516129, 0.032258064516129, 0.032258064516129,
			0.032258064516129, 0.032258064516129, 0.032258064516129,
			0.032258064516129, 0.032258064516129, 0.032258064516129,
			0.032258064516129, 0.032258064516129, 0.032258064516129,
			0.032258064516129};
	/**
	 * Gauss quadrature length for the interval [-1,1] and N=11 steps.
	 * 
	 * @see http
	 *      ://processingjs.nihongoresources.com/bezierinfo/legendre-gauss-values
	 *      .php
	 */
	private static int GaussQuadratureN = 21;
	private static int NormalQuadratureN = 31;
	private static int ExponentialQuadratureN = 13;
	/**
	 * Integral over the two-dimensional mollifier with sigma = 1.0, from -1 to
	 * 1.
	 */
	private static double mollifierIntegral = 0.466512;

	/**
	 * Compute the gradient at the given position x, using a mollified version
	 * of the solution stored in the potential field.
	 * 
	 * @param pot
	 * @param x
	 * @param grad
	 */
	public static void getGradientMollified(CellGrid pot, double[] x,
			double[] grad, double gradientMollifierRadius) {
		double aX = Math.max(0, x[0] - gradientMollifierRadius);
		double bX = Math.min(pot.getNumPointsX() - 1, x[0]
				+ gradientMollifierRadius);
		double aY = Math.max(0, x[1] - gradientMollifierRadius);
		double bY = Math.min(pot.getNumPointsY() - 1, x[1]
				+ gradientMollifierRadius);
		double[] xMoll = new double[2];
		double[] cgrad = new double[2];

		double[] weights = GaussQuadratureWeights;
		double[] points = GaussQuadraturePoints;
		int N = GaussQuadratureN;

		// initialize gradient, s.t. no other values from outside are present
		grad[0] = 0;
		grad[1] = 0;

		double[][] potvalues = new double[N][N];

		for (int row = 0; row < N; row++) {
			for (int col = 0; col < N; col++) {
				double cx = points[row] * (bX - aX) / 2;
				double cy = points[col] * (bY - aY) / 2;

				// get the gradient of the mollifier
				xMoll[0] = cx;
				xMoll[1] = cy;
				MathUtil.cutExpGrad2D(xMoll, gradientMollifierRadius, cgrad);

				// get the potential at the current position
				double potValue = InterpolationUtil
						.getValueByTriangleInterpolation(pot,
								(cx + x[0]) / pot.getResolution(), (cy + x[1])
										/ pot.getResolution());
				if (potValue > 9999 || Double.isInfinite(potValue)
						|| Double.isNaN(potValue)) {
					potValue = 9999;
				}

				potvalues[row][col] = potValue;

				// mollify
				grad[0] += -cgrad[0] * potValue / mollifierIntegral
						* weights[row] * weights[col];
				grad[1] += -cgrad[1] * potValue / mollifierIntegral
						* weights[row] * weights[col];
			}
		}

		/*
		 * String str = GeometryPrinter.grid2string(potvalues); try {
		 * GenerateReport.printDataFile(
		 * "output\\test_obstacle_potential_provider_continuouspot_single",
		 * str); } catch (IOException e) {
		 * e.printStackTrace(); }
		 */

	}
}

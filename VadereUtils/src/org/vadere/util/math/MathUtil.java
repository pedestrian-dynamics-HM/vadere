package org.vadere.util.math;

import java.awt.Point;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

import org.apache.commons.math3.complex.Complex;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * Math utilities not covered by java.lang.Math
 * 
 */
public class MathUtil {

	private final static List<Point> neumannNeighborhood = getNeumannNeighborhood(new Point(0, 0));

	// epsilon for finite differences see https://en.wikipedia.org/wiki/Numerical_differentiation#Practical_considerations_using_floating_point_arithmetic
	public static double EPSILON = Math.sqrt(Math.ulp(1.0));

	public static double toPositiveSmallestRadian(final double radian) {
		double result = radian;
		if(result < 0) {
			while (result < 0) {
				result += 2 * Math.PI;
			}
		}
		else if(result > 2 * Math.PI) {
			while (result > 2 * Math.PI) {
				result -= Math.PI;
			}
		}

		return result;
	}

	/**
	 * The two-norm3D of a vector.
	 * 
	 * @param vector
	 *        a double vector with arbitrary number of entries x1,x2,....
	 * @return sqrt(x1^2+x2^2+...)
	 */
	public static double norm2(double[] vector) {
		double result = 0;
		for (int i = 0; i < vector.length; i++) {
			result += vector[i] * vector[i];
		}
		return Math.sqrt(result);
	}

	/**
	 * The one-norm3D of a vector.
	 * 
	 * @param vector
	 *        a double vector with arbitrary number of entries x1,x2,....
	 * @return abs(x1)+abs(x2)+...
	 */
	public static double norm1(double[] vector) {
		double result = 0;
		for (int i = 0; i < vector.length; i++) {
			result += Math.abs(vector[i]);
		}
		return result;
	}

	public static double expAp(final double y) {
		double x = 1d + y / 256d;
		x *= x; x *= x; x *= x; x *= x;
		x *= x; x *= x; x *= x; x *= x;
		return x;
	}

	/**
	 * The value of the gradient of the 2D smooth function with compact support
	 * given by exp(1/((x/cut)^2-1)).
	 * 
	 * @param x
	 *        2D position where the gradient should be evaluated.
	 * @param cut
	 *        if abs(x)>cut, the function (and its gradient) is zero.
	 * @param grad
	 *        a 2D vector where the result is stored.
	 */
	public static void cutExpGrad2D(double[] x, double cut, double[] grad) {
		double absx = Math.sqrt(x[0] * x[0] + x[1] * x[1]) / cut;
		if (absx >= 1) {
			grad[0] = 0;
			grad[1] = 0;
		} else {
			double denom = absx * absx - 1;
			grad[0] = -2 * Math.exp(1 / denom) * x[0] / (cut * cut) * 1
					/ (denom * denom);
			grad[1] = -2 * Math.exp(1 / denom) * x[1] / (cut * cut) * 1
					/ (denom * denom);
		}
	}

	/**
	 * The value of the 2D smooth function with compact support given by
	 * exp(1/((x/cut)^2-1)).
	 * 
	 * @param x
	 *        2D position where the gradient should be evaluated.
	 * @param cut
	 *        if abs(x)>cut, the function is zero.
	 * @return
	 */
	public static double cutExp(double x, double cut) {
		double absx = Math.abs(x) / cut;
		if (absx >= 1) {
			return 0;
		} else {
			return Math.exp(1 / ((absx * absx) - 1));
		}
	}

	/**
	 * The value of the 2D smooth function with compact support given by
	 * exp(1/((x/cut)^(2p)-1)).
	 * 
	 * @param x
	 *        2D position where the gradient should be evaluated.
	 * @param cut
	 *        if abs(x)>cut, the function is zero.
	 * @return
	 */
	public static double cutExp(double x, double cut, double p) {
		double absx = Math.abs(x) / cut;
		if (absx >= 1) {
			return 0;
		} else {
			return Math.exp(1 / (Math.pow(absx, 2 * p) - 1));
		}
	}

	/**
	 * Adds a vector to another one.
	 * 
	 * @param vec
	 *        the initial vector that contains the solution later on.
	 * @param vecToAdd
	 *        the vector that should be added.
	 */
	public static void vecAdd(double[] vec, double[] vecToAdd) {
		for (int i = 0; i < vec.length; i++) {
			vec[i] += vecToAdd[i];
		}
	}

	/**
	 * Multiplies the given vector with a scalar.
	 * 
	 * @param vec
	 * @param scalar
	 */
	public static void mult(double[] vec, double scalar) {
		for (int i = 0; i < vec.length; i++) {
			vec[i] *= scalar;
		}
	}

	/**
	 * computes the "smallest distance on the circle" of the angle3D
	 * (x+v,x,x2-x)
	 * 
	 * @param x
	 *        the starting position
	 * @param v
	 *        the viewing direction
	 * @param x2
	 *        the position that should be evaluated
	 * @return a value between 0.0 and 1.0 that indicates how much of the
	 *         position is visible. 1.0 is fully visible, 0.0 is hidden.
	 */
	public static double smallestViewingAngle(double[] x, double[] v, double[] x2) {
		// compute the "smallest distance on the circle" between the two angles
		// double phi1 = Math.atan2(x2[1]-x[1], x2[0]-x[0]);
		// double phi2 = Math.atan2(v[1], v[0]);
		// double phi = Math.min(Math.abs(phi1-phi2), 2*Math.PI -
		// Math.abs(phi1-phi2));

		double aa = ((x[0] + v[0] - x2[0]) * (x[0] + v[0] - x2[0]) + (x[1]
				+ v[1] - x2[1])
				* (x[1] + v[1] - x2[1]));
		double b = Math.sqrt((x[0] - x2[0]) * (x[0] - x2[0]) + (x[1] - x2[1])
				* (x[1] - x2[1]));
		double c = Math.sqrt((v[0]) * (v[0]) + (v[1]) * (v[1]));
		if (c == 0.0) {
			c = 1e-10;
		}

		// this clamping is necessary as sometimes rounding errors get
		// the argument above / below -1/+1 which causes acos to result
		// in NaN.
		double arg = Math.min(1,
				Math.max(-1, (b * b + c * c - aa) / (2 * b * c)));
		double phi = Math.acos(arg);

		return phi;
	}

	/**
	 * Computes the portion of a position x2 that is visible from position x
	 * given the direction in v.
	 * 
	 * @param x
	 *        the starting position
	 * @param v
	 *        the viewing direction
	 * @param x2
	 *        the position that should be evaluated
	 * @return a value between 0.0 and 1.0 that indicates how much of the
	 *         position is visible. 1.0 is fully visible, 0.0 is hidden.
	 */
	public static double visiblePortion(double[] x, double[] v, double[] x2) {
		double portion = 0;

		double phi = smallestViewingAngle(x, v, x2);

		// double sx = (MathUtil.norm2(v)+1) * 2.0;
		// double sy = (MathUtil.norm2(v)+1) * 2.0;

		// compute the visible range portion
		double visibleRange = 0.6;
		// portion = Math.exp(-Math.pow(Math.hypot((x[0]-x2[0])/sx,
		// (x[1]-x2[1])/sy),2)) * Math.cos(phi * visibleRange);

		portion = 1 * Math.cos(phi * visibleRange);

		// make positive only
		portion = Math.max(0.0, portion);

		// make a smooth step function
		// portion = step(portion-0.3, 0.03);

		return portion;
	}

	/**
	 * A smooth "step" function with given sigma.
	 * 
	 * @param t
	 *        the value where the function should be evaluated
	 * @param sigma
	 *        the sigma for smoothing the step
	 * @return step(t)
	 */
	public static double step(double t, double sigma) {
		return 1 / (1 + Math.exp(-t / sigma));
	}

	/**
	 * If the vector is nonzero, sets it to the same direction with length = 1.
	 * 
	 * @param vec
	 */
	public static void normalize(double[] vec) {
		double norm = norm2(vec);
		if (norm > 0) {
			double newX = vec[0] / norm;
			double newY = vec[1] / norm;
			vec[0] = newX;
			vec[1] = newY;
		}
	}

	/**
	 * Generates quasi random numbers in two dimensions using a VERY simple
	 * algorithm.
	 * 
	 * @param rndSource
	 *        the random number source that is used in the algorithm.
	 * @param count
	 *        number of points in two dimensions that should be created
	 * @param width
	 *        width of the area where the points should be created
	 * @param height
	 *        height of the area where the points should be created
	 * @param randomFrac
	 *        0...1, specifies the amount that is added after the bin is
	 *        chosen
	 * @return an array with dimension [count][2]
	 */
	public static double[][] quasiRandom2D(Random rndSource, int count,
			double width, double height, double randomFrac) {
		if (count == 0) {
			count = 1;
		}

		double[][] result = new double[count][2];

		// generate a grid with twice the number of bins as we want results
		double areaSideLenPerPed = Math.sqrt((width * height) / (count));
		int binsX = (int) Math.max(1, Math.ceil((width) / areaSideLenPerPed));
		int binsY = (int) Math.max(1, Math.ceil((height) / areaSideLenPerPed));
		double sideLenX = width / binsX;
		double sideLenY = height / binsY;

		// create a coordinate list to draw from later
		List<SimpleEntry<Integer, Integer>> binCoodinates = new LinkedList<>();
		for (int i = 0; i < binsX; i++) {
			for (int k = 0; k < binsY; k++) {
				binCoodinates.add(new SimpleEntry<>(i, k));
			}
		}

		// randomly draw from the coordinate list and fill the result
		for (int i = 0; i < count; i++) {
			int nextIndex = rndSource.nextInt(binCoodinates.size());
			SimpleEntry<Integer, Integer> coords = binCoodinates
					.remove(nextIndex);
			result[i][0] = Math.min(width, coords.getKey() * sideLenX
					+ rndSource.nextDouble() * sideLenX * randomFrac);
			result[i][1] = Math.min(height, coords.getValue()
					* sideLenY + rndSource.nextDouble()
							* sideLenY * randomFrac);
		}

		return result;
	}

	/**
	 * Computes the cross product of two vectors and store it in the cross
	 * vector.
	 * 
	 * @param v1
	 *        vector 1
	 * @param v2
	 *        vector 2
	 * @param cross
	 *        3D vector that will contain the result: cross = (v1 x v2)
	 */
	public static void cross(double[] v1, double[] v2, double[] cross) {
		cross[0] = v1[1] * v2[2] - v1[2] * v2[1];
		cross[1] = v1[2] * v2[0] - v1[0] * v2[2];
		cross[2] = v1[0] * v2[1] - v1[1] * v2[0];
	}

	/**
	 * Returns a list of absolute coordinates which correspond to the Moore
	 * neighborhood of the given point p.
	 */
	public static List<Point> getMooreNeighborhood(Point p) {
		List<Point> mooreNeighborhood = new LinkedList<>();

		mooreNeighborhood.add(new Point(p.x + 1, p.y));
		mooreNeighborhood.add(new Point(p.x, p.y + 1));
		mooreNeighborhood.add(new Point(p.x + 1, p.y + 1));
		mooreNeighborhood.add(new Point(p.x - 1, p.y));
		mooreNeighborhood.add(new Point(p.x, p.y - 1));
		mooreNeighborhood.add(new Point(p.x - 1, p.y - 1));
		mooreNeighborhood.add(new Point(p.x + 1, p.y - 1));
		mooreNeighborhood.add(new Point(p.x - 1, p.y + 1));

		return mooreNeighborhood;
	}

	/**
	 * Returns a list of absolute coordinates which correspond to the Neumann
	 * neighborhood of the given point p.
	 */
	public static List<Point> getNeumannNeighborhood(final Point p) {
		List<Point> neumannNeighborhood = new LinkedList<>();
		neumannNeighborhood.add(new Point(p.x - 1, p.y));
		neumannNeighborhood.add(new Point(p.x + 1, p.y));
		neumannNeighborhood.add(new Point(p.x, p.y - 1));
		neumannNeighborhood.add(new Point(p.x, p.y + 1));
		return neumannNeighborhood;
	}

	/**
	 * Returns a list of relative offsets which correspond to the Neumann
	 * neighborhood.
	 */
	public static List<Point> getRelativeNeumannNeighborhood() {
		return neumannNeighborhood;
	}

	/**
	 * Returns the value of the sigmoid function.
	 */
	public static double sigmoid(double t) {
		return 1.0 / (1.0 + Math.exp(-t));
	}

	/**
	 * Returns the real solutions of the quadratic equation ax^2+bx+c=0
	 */
	public static List<Double> solveQuadratic(double a, double b, double c) {
		ArrayList<Double> result = new ArrayList<>(2);
		if (a != 0) {
			double discr = (b * b) - (4 * a * c);

			// one solution
			if (discr == 0) {
				Collections.addAll(result, -b / (2.0 * a));
			} else if (discr > 0) {
				Collections.addAll(result, (-b + Math.sqrt(discr)) / (2.0 * a), (-b - Math.sqrt(discr)) / (2.0 * a));
			}
		} else if (b != 0) {
			result.add(-c / b);
		} else {
			//throw new IllegalArgumentException("ax^2 + bx + c = 0 is not a valid quadratic equation for a=b=0.");
		}

		return result;
	}

	/**
	 * Returns the max real solutions of the quadratic equation ax^2+bx+c=0 or Double.MAX_VALUE if there is no solution.
	 */
	public static double solveQuadraticMax(double a, double b, double c) {
		if (a != 0) {
			double discr = (b * b) - (4 * a * c);

			// one solution
			if (discr == 0) {
				return -b / (2.0 * a);
			} else if (discr > 0) {
				return Math.max((-b + Math.sqrt(discr)) / (2.0 * a), (-b - Math.sqrt(discr)) / (2.0 * a));
			}
		} else if (c != 0) {
			return -c / b;
		} else {
			throw new IllegalArgumentException("ax^2 + bx + c = 0 is not a valid quadratic equation for a=b=0.");
		}

		return Double.MAX_VALUE;
	}

	/**
	 * Returns true if the point p lies on the left of the vector defined by ab.
	 */
	public static boolean pOverLineAB(VPoint p, VPoint a, VPoint b) {

		double criteria = (p.x - a.x) * (a.y - b.y) + (p.y - a.y) * (b.x - a.x);

		return criteria > 0;
	}

	public static VPoint proectVector(final VPoint origin,
			final VPoint projection) {
		double tmp = (origin.x * projection.x + origin.y * projection.y)
				/ (projection.x * projection.x + projection.y * projection.y);
		return new VPoint(projection.x * tmp, projection.y * tmp);
	}

	/**
	 * Transforms an array of Complex numbers to a double array of doubled length. At even indicies
	 * there are the real parts and at odd indicies are the imaginary parts of the complex numbers.
	 *
	 * @param complex a array of Complex numbers
	 * @return a double array representing the same complex numbers
	 */
	public static double[] toDouble(final Complex[] complex) {
		double[] pairs = new double[complex.length*2];
		for(int i = 0; i < complex.length*2; i += 2) {
			pairs[i] = complex[i/2].getReal();
			pairs[i+1] = complex[i/2].getImaginary();
		}
		return pairs;
	}

	/**
	 * Transforms an array of Complex numbers to a float array of doubled length. At even indicies
	 * there are the real parts and at odd indicies are the imaginary parts of the complex numbers.
	 *
	 * @param complex a array of Complex numbers
	 * @return a float array representing the same complex numbers
	 */
	public static float[] toFloat(final Complex[] complex) {
		float[] pairs = new float[complex.length*2];
		for(int i = 0; i < complex.length*2; i += 2) {
			pairs[i] = (float)complex[i/2].getReal();
			pairs[i+1] = (float)complex[i/2].getImaginary();
		}
		return pairs;
	}

	/**
	 * Transform the real numbers to complex numbers i.e. the imaginary part is zero.
	 * @param realValues
	 * @return a complex array representing the same real numbers
	 */
	public static Complex[] toComplex(final double[] realValues) {
		Complex[] complex = new Complex[realValues.length];
		for(int i = 0; i < realValues.length; i++) {
			complex[i] = Complex.valueOf(realValues[i], 0.0);
		}
		return complex;
	}

	public static double clamp(double val, double min, double max) {
		return Math.max(min, Math.min(max, val));
	}

}

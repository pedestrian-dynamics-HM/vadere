package org.vadere.util.math.optimization.neldermead;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;

import java.util.Arrays;
import java.util.function.Function;

/**
 * @author Benedikt Zoennchen
 */
public class Simplex2D {
	private static final Logger logger = Logger.getLogger(Simplex2D.class);

	/**
	 * (sorted) three points of the simplex i.e. the defined triangle.
	 */
	private VPoint[] points;

	/**
	 * (sorted) three values of the simplex i.e. the function values at the points.
	 * The first value is the best, the second the second best and the third is the worst value.
	 */
	private double[] values;

	/**
	 * parameters of the Nelder-Mead method.
	 */
	private double shrinkFactor = 0.5;
	private double projectionFactor = 1;
	private double expansionFactor = 2;
	private double contractionFactor = 0.5;

	/**
	 * true if and only if this simplex minimizes the function.
	 */
	private boolean minimize;

	/**
	 * the evaluation function which is optimized.
	 */
	private final Function<IPoint,Double> eval;

	/**
	 * true, if points or values changes without recomputing the square mean or the area.
	 */
	private boolean changed;

	/**
	 * the current variance of the values of the simplex
	 */
	private double variance;

	/**
	 * the current area of the area, which has to be recomputed if the simplex changes
	 */
	private double area;

	private double edgeLen;
	private double maxArea = 0.01;
	private double maxEdgeLen = 0.01;

	/**
	 * see See https://e-maxx.ru/bookz/files/numerical_recipes.pdf page 506
	 */
	private static final double TINY = 1.0E-10;

	/**
	 * values outside of the step circle should give eval.apply() == invalidValue
	 */
	private double invalidValue;


	public Simplex2D(@NotNull final Function<IPoint,Double> eval, @NotNull final VTriangle triangle) {
		this(eval, triangle, true, NelderMead2D.MAX_VAL);
	}

	public Simplex2D(@NotNull final Function<IPoint,Double> eval, @NotNull final VTriangle triangle, final boolean minimize, double invalidValue) {
		this.points = new VPoint[3];
		this.values = new double[3];
		this.points[0] = triangle.p1;
		this.points[1] = triangle.p2;
		this.points[2] = triangle.p3;
		this.eval = eval;
		this.minimize = minimize;
		this.changed = true;
		this.invalidValue = invalidValue;
		refresh();
		sort();
	}

	/**
	 * performs one Nelder-Mead iteration.
	 */
	public void update() {
		//0 => best, 1 => second worst, 2 => worst

		changed = true;
		var x = new VPoint(0,0);
		for(var i = 0; i < 2; i++) {
			x = x.add(points[i]);
		}

		// x = centroid point
		x = x.scalarMultiply(0.5);

		// xr = reflected point
		var xr = x.add(x.subtract(points[2]).scalarMultiply(projectionFactor));
		var fr = eval.apply(xr);

		if(isBetter(fr, values[1]) && isWorse(fr, values[0])) {
			//logger.info("reflect");
			points[2] = xr;
			values[2] = fr;
		} else if(isBetter(fr, values[0])) {
			var xe = x.add(xr.subtract(x).scalarMultiply(expansionFactor));
			var fe = eval.apply(xe);
			if(isBetter(fe, fr)) {
				//logger.info("expand");
				points[2] = xe;
				values[2] = fe;
			} else {
				//logger.info("reflect");
				points[2] = xr;
				values[2] = fr;
			}
		// contraction!
		} else if(isBetter(fr, values[2])) {
			var xoc = x.add(xr.subtract(x).scalarMultiply(contractionFactor));
			var foc = eval.apply(xoc);

			if(isBetter(foc, fr)) {
				points[2] = xoc;
				values[2] = foc;
			}
			else {
				shrink();
			}
		} else {
			var xic = x.subtract(x.subtract(points[2]).scalarMultiply(contractionFactor));
			var fic = eval.apply(xic);

			if(isBetter(fic, values[2])) {
				points[2] = xic;
				values[2] = fic;
			}
			else {
				shrink();
			}
		}
		sort();
	}

	/**
	 * See https://e-maxx.ru/bookz/files/numerical_recipes.pdf page 503
	 */
	public void restart(double delta) {
		points[1] = points[0].add(delta, 0);
		points[2] = points[1].add(0, delta);
		values[1] = eval.apply(points[1]);
		values[2] = eval.apply(points[2]);
	}

	/**
	 * Returns true if the simplex runs into an invalid area i.e. more than 1 values reach the max.
	 *
	 * @return true if the simplex runs into an invalid area i.e. more than 1 values reach the max
	 */
	public boolean isInvalid() {
		return values[1] == invalidValue && values[2] == invalidValue;
	}

	/**
	 * True if the size of the simplex and the variance of values is below the <tt>threshold</tt>.
	 * Or the globally best point has been found.
	 *
	 * @param threshold the threshold
	 * @return true if this simplex has converged, false otherwise
	 */
	public boolean hasConverged(final double threshold) {
		if(values[0] == NelderMead2D.SOLUTION_VAL) {
			return true;
		}

		if(changed) {
			variance = variance();
			area = getArea();
			//edgeLen = getMaxEdgeLen();
			changed = false;
		}
		return variance < threshold && area <= threshold/* && edgeLen <= maxEdgeLen*/;
	}

	/**
	 * Returns the best value of the current simplex.
	 *
	 * @return the best value of the current simplex
	 */
	public double getValue() {
		return values[0];
	}

	/**
	 * Returns the argument of the best simplex value.
	 *
	 * @return the argument of the best simplex value
	 */
	public VPoint getArg() {
		return points[0];
	}

	/**
	 * Returns the mean of values of the simplex.
	 *
	 * @return the mean of values of the simplex
	 */
	private double mean() {
		return (values[0] + values[1] + values[2]) / 3.0;
	}

	/**
	 * Computes and returns the area of the simplex.
	 *
	 * @return the area of the simplex
	 */
	private double getArea() {
		if(points[0].equals(points[1]) || points[0].equals(points[2]) || points[1].equals(points[2])) {
			return 0.0;
		}
		else {
			return GeometryUtils.areaOfPolygon(Arrays.asList(points));
		}
	}

	/**
	 * Returns the length of the largest edge length of the simplex
	 *
	 * @return the length of the largest edge length of the simplex
	 */
	private double getMaxEdgeLen() {
		return Math.sqrt(Math.max(Math.max(points[0].distanceSq(points[1]), points[0].distanceSq(points[2])), points[1].distanceSq(points[2])));
	}

	/**
	 * See https://e-maxx.ru/bookz/files/numerical_recipes.pdf page 506
	 * TODO: should we use this?
	 * @return
	 */
	private double getTol() {
		return 2.0*Math.abs(values[2] - values[0]) / (Math.abs(values[2]) + Math.abs(values[0]) + TINY);
	}

	/**
	 * Returns the variance of the values of the simplex.
	 *
	 * @return the variance of the values of the simplex
	 */
	private double variance() {
		double mean = mean();
		double v1 = values[0] - mean;
		double v2 = values[1] - mean;
		double v3 = values[2] - mean;

		return (v1*v1 + v2*v2 + v3*v3) / 3;
	}

	/**
	 * Returns true if and only if d1 is worse than d2
	 *
	 * @param d1
	 * @param d2
	 * @return true if and only if d1 is worse than d2
	 */
	private boolean isWorse(final double d1, final double d2) {
		if(minimize) {
			return d1 > d2;
		} else {
			return d1 < d2;
		}
	}

	/**
	 * Returns true if and only if d1 is better than d2, i.e. for minimization: d1 < d2
	 *
	 * @param d1
	 * @param d2
	 * @return true if and only if d1 is better than d2
	 */
	private boolean isBetter(final double d1, final double d2) {
		if(minimize) {
			return d1 < d2;
		}
		else {
			return d1 > d2;
		}
	}

	/**
	 * shrinks the simplex geometrically.
	 */
	private void shrink() {
		//logger.info("shrink");
		for(var i = 1; i < 3; i++) {
			VPoint dir = points[i].subtract(points[0]);
			points[i] = points[0].add(dir).scalarMultiply(shrinkFactor);
			values[i] = eval.apply(points[i]);
		}
	}

	/**
	 * computes and sets all values by evaluating the evaluation function for each point.
	 */
	private void refresh() {
		for(var i = 0; i < 3; i++) {
			values[i] = eval.apply(points[i]);
		}
		changed = true;
	}

	/**
	 * sorts values and points such that value[0] <= value[1] <= value[2].
	 */
	private void sort() {
		// bubble sort
		if(isWorse(values[0], values[1])) {
			swap(0,1);
		}

		if(isWorse(values[1], values[2])) {
			swap(1,2);
		}

		if(isWorse(values[0], values[1])) {
			swap(0,1);
		}
	}

	/**
	 * swaps values and point array at positions i and j.
	 *
	 * @param i first swap position
	 * @param j second swap position
	 */
	private void swap(final int i, final int j) {
		var s = points[i];
		var v = values[i];
		points[i] = points[j];
		values[i] = values[j];
		points[j] = s;
		values[j] = v;
	}

	@Override
	public String toString() {
		return "[["+points[0].x+","+points[0].y+"],["+points[1].x+","+points[1].y+"],["+points[2].x+","+points[2].y+"]]";
	}
}

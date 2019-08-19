package org.vadere.util.math.optimization.neldermead;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.function.Function;

/**
 * @author Benedikt Zoennchen
 */
public class Simplex1D {

	/**
	 * (sorted) two points of the simplex i.e. the defined line.
	 */
	private double[] rads;

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
	 * the current distance of the two points defining the simplex.
	 */
	private double distance;

	/**
	 * see See https://e-maxx.ru/bookz/files/numerical_recipes.pdf page 506
	 */
	private static final double TINY = 1.0E-10;

	/**
	 * the step circle.
	 */
	private final VCircle circle;

	/**
	 * values outside of the step circle should give eval.apply() == invalidValue
	 */
	private final double invalidValue;

	public Simplex1D(@NotNull final Function<IPoint,Double> eval, @NotNull final double rad1, final double rad2, @NotNull final VCircle circle) {
		this(eval, rad1, rad2, circle, true, NelderMead2D.MAX_VAL);
	}

	public Simplex1D(@NotNull final Function<IPoint,Double> eval, double rad1, double rad2, @NotNull final VCircle circle, final boolean minimize, double invalidValue) {
		this.rads = new double[]{rad1, rad2};
		this.values = new double[2];
		this.eval = eval;
		this.minimize = minimize;
		this.changed = true;
		this.circle = circle;
		this.invalidValue = invalidValue;
		refresh();
		sort();
	}

	/**
	 * performs one Nelder-Mead iteration.
	 */
	public void update() {
		changed = true;
		var x = rads[0];
		var xr = x + projectionFactor * (x - rads[1]);
		var fr = eval(xr);

		if(isBetter(fr, values[1]) && isWorse(fr, values[0])) {
			rads[1] = xr;
			values[1] = fr;
		} else if(isBetter(fr, values[0])) {
			var xe = x + expansionFactor * (xr - x);
			var fe = eval(xe);
			var xoc = x + contractionFactor * (xr - x);
			var foc = eval(xoc);
			if(isBetter(fe, fr) && isBetter(fe, foc)) {
				rads[1] = xe;
				values[1] = fe;
			} else if(isBetter(foc, fr)) {
				rads[1] = xoc;
				values[1] = foc;
			} else {
				rads[1] = xr;
				values[1] = fr;
			}
		}  else {
			var xic = x - contractionFactor * (rads[1] -x);
			var fic = eval(xic);
			if(isBetter(fic, fr)) {
				rads[1] = xic;
				values[1] = fic;
			} else {
				shrink();
			}
		}
		sort();
	}

	private double eval(final double rad) {
		/*if(rad < 0 || rad > 2*Math.PI) {
			return minimize ? 10000 : -10000;
		}*/
		return eval.apply(toPoint(rad));
	}

	private VPoint toPoint(final double rad) {
		return new VPoint(Math.cos(rad) * circle.getRadius(), Math.sin(rad) * circle.getRadius()).add(circle.getCenter());
	}

	/**
	 * See https://e-maxx.ru/bookz/files/numerical_recipes.pdf page 503
	 */
	public void restart(double delta) {
		rads[1] = rads[0] + delta;
		values[1] = eval(rads[1]);
		sort();
	}

	/**
	 * Returns true if the simplex runs into an invalid area i.e. 1 value reach the max.
	 *
	 * @return true if the simplex runs into an invalid area i.e. 1 value reach the max.
	 */
	public boolean isInvalid() {
		return values[0] >= invalidValue;
	}

	/**
	 * True if the geometrically size of the simplex and the variance of values is below the <tt>threshold</tt>.
	 * Or the globally best point has been found.
	 *
	 * @param threshold the threshold
	 * @return true if this simplex has converged, false otherwise
	 */
	public boolean hasConverged(final double threshold) {
		if(changed) {
			variance = variance();
			distance = getDistance();
			changed = false;
		}
		return variance < threshold && distance < threshold;
		//return getTol() < threashold;
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
	 * Returns the argument of the best simplex value (in rad).
	 *
	 * @return the argument of the best simplex value
	 */
	public VPoint getArg() {
		return toPoint(rads[0]);
	}

	/**
	 * Returns the mean of values of the simplex.
	 *
	 * @return the mean of values of the simplex
	 */
	private double mean() {
		return (values[0] + values[1]) / 2.0;
	}

	/**
	 * Computes and returns the geometrically size of the simplex.
	 *
	 * @return the area of the simplex
	 */
	private double getDistance() {
		double d1 = (rads[0] % (2*Math.PI));
		double d2 = (rads[1] % (2*Math.PI));
		return Math.abs(d1 - d2);
	}

	/**
	 * See https://e-maxx.ru/bookz/files/numerical_recipes.pdf page 506
	 * @return
	 */
	private double getTol() {
		return 2.0*Math.abs(values[1] - values[0]) / (Math.abs(values[1]) + Math.abs(values[0]) + TINY);
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

		return (v1*v1 + v2*v2) / 2;
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
		for(var i = 1; i < 2; i++) {
			rads[i] = rads[0] + shrinkFactor * (rads[i] - rads[0]);
			values[i] = eval(rads[i]);
		}
	}

	/**
	 * computes and sets all values by evaluating the evaluation function for each point.
	 */
	private void refresh() {
		for(var i = 0; i < 2; i++) {
			values[i] = eval(rads[i]);
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
	}

	/**
	 * swaps values and point array at positions i and j.
	 *
	 * @param i first swap position
	 * @param j second swap position
	 */
	private void swap(final int i, final int j) {
		var s = rads[i];
		var v = values[i];
		rads[i] = rads[j];
		values[i] = values[j];
		rads[j] = s;
		values[j] = v;
	}
}

package org.vadere.util.math.optimizer.neldermead;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.function.Function;

/**
 * @author Benedikt Zoennchen
 */
public class Simplex1D {
	private double[] rads;
	private double[] values;
	private double shrinkFactor = 0.5;
	private double projectionFactor = 1;
	private double expansionFactor = 2;
	private double contractionFactor = 0.5;
	private boolean minimize;
	private final Function<IPoint,Double> eval;
	private boolean changed;
	private double sqrtMean;
	private double distance;
	private static final double TINY = 1.0E-10;
	private final VCircle circle;


	public Simplex1D(@NotNull final Function<IPoint,Double> eval, @NotNull final double rad1, final double rad2, @NotNull final VCircle circle) {
		this(eval, rad1, rad2, circle, true);
	}

	public Simplex1D(@NotNull final Function<IPoint,Double> eval, double rad1, double rad2, @NotNull final VCircle circle, final boolean minimize) {
		this.rads = new double[]{rad1, rad2};
		this.values = new double[2];
		this.eval = eval;
		this.minimize = minimize;
		this.changed = true;
		this.circle = circle;
		refresh();
	}

	public void update() {
		changed = true;
		sort();
		var x = rads[0];
		var xr = x + projectionFactor * (x - rads[1]);
		var fr = eval(xr);

		if(isBetter(fr, values[1]) && isWorse(fr, values[0])) {
			rads[1] = xr;
		} else if(isBetter(fr, values[0])) {
			var xe = x + expansionFactor * (xr - x);
			var fe = eval(xe);
			var xoc = x + contractionFactor * (xr - x);
			var foc = eval(xoc);
			if(isBetter(fe, fr) && isBetter(fe, foc)) {
				rads[1] = xe;
			} else if(isBetter(foc, fr)) {
				rads[1] = xoc;
			} else {
				rads[1] = xr;
			}
		}  else {
			var xic = x - contractionFactor * (rads[1] -x);
			var fic = eval(xic);
			if(isBetter(fic, fr)) {
				rads[1] = xic;
			} else {
				shrink();
			}
		}
		refresh();
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
		refresh();
	}

	public boolean hasConverged(final double threashold) {
		if(changed) {
			sqrtMean = sqrtMean();
			distance = getDistance();
			changed = false;
		}
		return sqrtMean < threashold && distance < threashold;
		//return getTol() < threashold;
	}

	public double getValue() {
		return values[0];
	}

	public VPoint getArg() {
		return toPoint(rads[0]);
	}

	private double mean() {
		return (values[0] + values[1]) / 2.0;
	}

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

	private double sqrtMean() {
		double mean = mean();
		double v1 = values[0] - mean;
		double v2 = values[1] - mean;

		return (v1*v1 + v2*v2) / 2;
	}

	private boolean isWorse(final double d1, final double d2) {
		if(minimize) {
			return d1 >= d2;
		} else {
			return d1 <= d2;
		}
	}

	private boolean isBetter(final double d1, final double d2) {
		if(minimize) {
			return d1 < d2;
		}
		else {
			return d1 > d2;
		}
	}

	private void shrink() {
		for(var i = 1; i < 2; i++) {
			rads[i] = rads[0] + shrinkFactor * (rads[i] - rads[0]);
		}
	}

	private void refresh() {
		for(var i = 0; i < 2; i++) {
			values[i] = eval(rads[i]);
		}
	}

	private void sort() {
		// bubble sort
		if(isWorse(values[0], values[1])) {
			swap(0,1);
		}
	}

	private void swap(final int i, final int j) {
		var s = rads[i];
		var v = values[i];
		rads[i] = rads[j];
		values[i] = values[j];
		rads[j] = s;
		values[j] = v;
	}
}

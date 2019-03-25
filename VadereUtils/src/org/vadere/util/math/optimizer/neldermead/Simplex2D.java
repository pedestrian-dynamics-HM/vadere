package org.vadere.util.math.optimizer.neldermead;

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
	private VPoint[] points;
	private double[] values;
	private double shrinkFactor = 0.5;
	private double projectionFactor = 1;
	private double expansionFactor = 2;
	private double contractionFactor = 0.5;
	private boolean minimize;
	private final Function<IPoint,Double> eval;
	private boolean changed;
	private double sqrtMean;
	private double area;
	private double edgeLen;
	private double maxArea = 0.01;
	private double maxEdgeLen = 0.01;
	private static final double TINY = 1.0E-10;
	private final double MAX_VAL = 1000;


	public Simplex2D(@NotNull final Function<IPoint,Double> eval, @NotNull final VTriangle triangle) {
		this(eval, triangle, true);
	}

	public Simplex2D(@NotNull final Function<IPoint,Double> eval, @NotNull final VTriangle triangle, final boolean minimize) {
		this.points = new VPoint[3];
		this.values = new double[3];
		this.points[0] = triangle.p1;
		this.points[1] = triangle.p2;
		this.points[2] = triangle.p3;
		this.eval = eval;
		this.minimize = minimize;
		this.changed = true;
		refresh();
	}

	public void update() {
		changed = true;
		this.sort();
		var x = new VPoint(0,0);
		for(var i = 0; i < 2; i++) {
			x = x.add(points[i]);
		}
		x = x.scalarMultiply(0.5);
		var xr = x.add(x.subtract(points[2]).scalarMultiply(projectionFactor));
		var fr = eval.apply(xr);

		if(isBetter(fr, values[1]) && isWorse(fr, values[0])) {
			//logger.info("reflect");
			points[2] = xr;
		} else if(isBetter(fr, values[0])) {
			var xe = x.add(xr.subtract(x).scalarMultiply(expansionFactor));
			var fe = eval.apply(xe);
			if(isBetter(fe, fr)) {
				//logger.info("expand");
				points[2] = xe;
			} else {
				//logger.info("reflect");
				points[2] = xr;
			}
		} else if(isBetter(fr, values[2]) && isWorse(fr, values[1])) {
			var xoc = x.add(xr.subtract(x).scalarMultiply(contractionFactor));
			var foc = eval.apply(xoc);
			if(isBetter(foc, fr)) {
				//logger.info("contract (1)");
				points[2] = xoc;
			} else {
				shrink();
			}
		} else {
			var xic = x.subtract(points[2].subtract(x).scalarMultiply(contractionFactor));
			var fic = eval.apply(xic);
			if(isBetter(fic, fr)) {
				//logger.info("contract (2)");
				points[2] = xic;
			} else {
				shrink();
			}
		}
		refresh();
	}

	/**
	 * See https://e-maxx.ru/bookz/files/numerical_recipes.pdf page 503
	 */
	public void restart(double delta) {
		points[1] = points[0].add(delta, 0);
		points[2] = points[1].add(0, delta);
		refresh();
	}

	public boolean hasConverged(final double threashold) {
		if(values[0] >= MAX_VAL && values[1] >= MAX_VAL && values[2] >= MAX_VAL) {
			//logger.warn("illegal initial simplex");
			// abort!
			return true;
		}

		if(changed) {
			sqrtMean = sqrtMean();
			area = getArea();
			//edgeLen = getMaxEdgeLen();
			changed = false;
		}
		return sqrtMean < threashold && area <= threashold/* && edgeLen <= maxEdgeLen*/;
	}

	public double getValue() {
		return values[0];
	}

	public VPoint getArg() {
		return points[0];
	}

	private double mean() {
		return (values[0] + values[1] + values[2]) / 3.0;
	}

	private double getArea() {
		if(points[0].equals(points[1]) || points[0].equals(points[2]) || points[1].equals(points[2])) {
			return 0.0;
		}
		else {
			return GeometryUtils.areaOfPolygon(Arrays.asList(points));
		}
	}

	private double getMaxEdgeLen() {
		return Math.sqrt(Math.max(Math.max(points[0].distanceSq(points[1]), points[0].distanceSq(points[2])), points[1].distanceSq(points[2])));
	}

	/**
	 * See https://e-maxx.ru/bookz/files/numerical_recipes.pdf page 506
	 * @return
	 */
	private double getTol() {
		return 2.0*Math.abs(values[2] - values[0]) / (Math.abs(values[2]) + Math.abs(values[0]) + TINY);
	}

	private double sqrtMean() {
		double mean = mean();
		double v1 = values[0] - mean;
		double v2 = values[1] - mean;
		double v3 = values[2] - mean;

		return (v1*v1 + v2*v2 + v3*v3) / 3;
	}

	private boolean isWorse(final double d1, final double d2) {
		if(minimize) {
			return d1 > d2;
		} else {
			return d1 < d2;
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
		//logger.info("shrink");
		for(var i = 1; i < 3; i++) {
			points[i] = points[0].add(points[i].subtract(points[0]).scalarMultiply(shrinkFactor));
		}
	}

	private void refresh() {
		for(var i = 0; i < 3; i++) {
			values[i] = eval.apply(points[i]);
		}
		changed = true;
	}

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

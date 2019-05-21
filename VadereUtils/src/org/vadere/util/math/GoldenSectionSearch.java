package org.vadere.util.math;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Recursive implementation of the Golden section search algorithm.
 *
 * @author Benedikt Zoennchen
 */
public class GoldenSectionSearch {
	private static final double invphi = (Math.sqrt(5.0)-1)/2.0;
	private static final double invphi2 = (3-Math.sqrt(5.0))/2.0;

	/**
	 * Computes a subinterval [a1;b1] of [a;b] such that the minimum of f is contained in [a1;b1].
	 * The tolerance <tt>tol</tt> controls the accuracy, that is the size of the subinterval.
	 * This method uses recursion.
	 *
	 * @param f     the function to be minimized
	 * @param a     defines the interval. Assumption: a < b
	 * @param b     defines the interval. Assumption: b > a
	 * @param tol   controls the accuracy, i.e. b1-a1 <= tol
	 *
	 * @return a subinterval [a1;b1] containing the minimum of f
	 */
	public static double[] gss(@NotNull final Function<Double, Double> f, final double a, final double b, final double tol) {
		return gss(f, a, b, tol,b - a,true,0,0,true,0,0);
	}

	private static double[] gss(@NotNull Function<Double, Double> f,
	                            final double a, final double b, final double tol,
	                            double h, boolean noC, double c, double fc,
	                            boolean noD, double d, double fd) {
		if (Math.abs(h) <= tol) {
			return new double[] { a, b };
		}

		if (noC) {
			c = a + invphi2 * h;
			fc = f.apply(c);
		}

		if (noD) {
			d = a + invphi * h;
			fd = f.apply(d);
		}

		if (fc < fd) {
			return gss(f, a, d, tol,h * invphi,true,0,0,false, c, fc);
		} else {
			return gss(f, c, b, tol,h * invphi,false, d, fd,true,0,0);
		}
	}
}


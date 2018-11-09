/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vadere.simulator.models.potential.solver.calculators;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import org.vadere.util.geometry.Vector3D;
import org.vadere.util.data.cellgrid.CellStateFD;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction3D;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction3D;

/**
 * Uses the fast-marching algorithm to solve the Eikonal equation (F(x) * |grad
 * phi|=1) in a given 3D room.
 * 
 */
public class PotentialFieldCalculatorFastMarching3D {

	private static class DistPoint3D implements Comparable<DistPoint3D> {

		public Vector3D p;
		public double dist;
		private double epsCompare = 1e-6;

		public DistPoint3D(Vector3D p, double dist) {
			this.p = p;
			this.dist = dist;
		}

		@Override
		public int compareTo(DistPoint3D o) {
			if (Math.abs(this.dist - o.dist) < epsCompare) {
				return this.p.compareTo(o.p);
			} else if (this.dist < o.dist) {
				return -1;
			}
			return 1;
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 29 * hash + (this.p != null ? this.p.hashCode() : 0);
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final DistPoint3D other = (DistPoint3D) obj;
			if (this.p != other.p
					&& (this.p == null || !this.p.equals(other.p))) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			NumberFormat fmt = NumberFormat.getInstance(Locale.ENGLISH);
			return String.format("%s@%s", p.toString(), fmt.format(dist));
		}
	}

	private Vector3D[] neighborPoints = new Vector3D[] {
			new Vector3D(-1, 0, 0), new Vector3D(1, 0, 0),
			new Vector3D(0, -1, 0), new Vector3D(0, 1, 0),
			new Vector3D(0, 0, -1), new Vector3D(0, 0, 1),};
	private ITimeCostFunction3D timeCostFunction;

	private Vector3D neighbors(int index) {
		return new Vector3D(neighborPoints[index].x, neighborPoints[index].y,
				neighborPoints[index].z);
	}

	/**
	 * Initializes the FM potential calculator with a time cost function F > 0.
	 * 
	 * @param timeCostFunction
	 *        an ITimeCostFunction, that defines the time cost ( > 0 ) at
	 *        every point on the grid.
	 */
	public PotentialFieldCalculatorFastMarching3D(
			ITimeCostFunction3D timeCostFunction) {
		this.timeCostFunction = timeCostFunction;
	}

	/**
	 * Initializes the FM potential calculator with a time cost function F = 1.
	 */
	public PotentialFieldCalculatorFastMarching3D() {
		this.timeCostFunction = new UnitTimeCostFunction3D();
	}

	/**
	 * Calculate the new potential for all targets using the Eikonal equation.
	 * 
	 * @param potential
	 * @param elements
	 * @param targets
	 */
	public double[][][] recalculate(double[][][] potential,
			CellStateFD[][][] elements, List<Vector3D> targets) {
		TreeSet<Vector3D> frozenPoints = new TreeSet<Vector3D>();
		TreeSet<DistPoint3D> narrowBand = new TreeSet<DistPoint3D>();

		// initialize the potential to infty
		for (int i = 0; i < potential.length; i++) {
			for (int j = 0; j < potential[i].length; j++) {
				for (int k = 0; k < potential[i][j].length; k++) {
					potential[i][j][k] = Double.MAX_VALUE;
				}
			}
		}

		// set target potential to zero
		// add to frozen points
		for (Vector3D t : targets) {
			int x = (int) Math.round(t.x);// / Geometry.getCellWidth());
			int y = (int) Math.round(t.y);// / Geometry.getCellWidth());
			int z = (int) Math.round(t.z);// / Geometry.getCellWidth());

			potential[x][y][z] = 0;
			frozenPoints.add(t);
		}

		// near points are in the moore area around the targets
		for (Vector3D t : targets) {
			addNearPoints(t, narrowBand, frozenPoints, elements, potential);
		}

		DistPoint3D nearPoint;
		// while there are near points to generate the potential at, generate it
		while (narrowBand.size() > 0) {
			// get the point with lowest distance
			nearPoint = narrowBand.pollFirst();
			// boolean removed = narrowBand.remove(nearPoint);
			frozenPoints.add(nearPoint.p);

			addNearPoints(nearPoint.p, narrowBand, frozenPoints, elements,
					potential);
		}

		return potential;
	}

	/**
	 * Recalculates the potential at the given point, using the neigbor points
	 * and the high accuracy version.
	 * 
	 * @param p
	 *        point to recalculate the potential at
	 * @param frozenPoints
	 *        all frozen points so far
	 * @param potential
	 *        potential so far
	 * @return potential value of the point
	 */
	private double recalculateAt(Vector3D p, TreeSet<Vector3D> frozenPoints,
			double[][][] potential) {
		int x = (int) (p.x);
		int y = (int) (p.y);
		int z = (int) (p.z);
		int width = potential.length;
		int height = potential[0].length;
		int depth = potential[0][0].length;
		double val1;
		double val2;

		double cost = timeCostFunction.costAt(p);
		double[] coeff = new double[] {-1 / (cost * cost), 0, 0};

		// for all directions, add up the differences
		for (int j = 0; j < 3; j++) {
			val1 = Double.MAX_VALUE;
			val2 = Double.MAX_VALUE;

			// for direction + and -
			for (int i = 0; i < 2; i++) {
				Vector3D pk = neighbors(j * 2 + i).add(new Vector3D(x, y, z));

				if (pk.x >= 0 && pk.y >= 0 && pk.z >= 0 && pk.x < width
						&& pk.y < height && pk.z < depth) {
					if (frozenPoints.contains(pk)) {
						double _val1 = potential[(int) pk.x][(int) pk.y][(int) pk.z];
						if (_val1 < val1) {
							val1 = _val1;

							Vector3D pk2 = neighbors(j * 2 + i);
							pk2 = pk2.multiply(2).add(new Vector3D(x, y, z));

							double _val2 = Double.MAX_VALUE;
							if (pk2.x >= 0 && pk2.y >= 0 && pk2.z >= 0
									&& pk2.x < width && pk2.y < height
									&& pk2.z < depth) {
								_val2 = potential[(int) pk2.x][(int) pk2.y][(int) pk2.z];
								if (frozenPoints.contains(new Vector3D(
										(int) pk2.x, (int) pk2.y, (int) pk2.z))
										&& _val2 <= _val1) {
									val2 = _val2;
								} else {
									val2 = Double.MAX_VALUE;
								}
							}
						}
					}
				}
			}

			// high accuracy
			if (val2 != Double.MAX_VALUE) {
				double tp = (1.0 / 3) * (4 * val1 - val2);
				double a = 9.0 / 4;
				coeff[2] += a;
				coeff[1] -= 2 * a * tp;
				coeff[0] += a * (tp) * tp;
			} else // low accuracy
			if (val1 != Double.MAX_VALUE) {
				coeff[2] += 1;
				coeff[1] -= 2 * val1;
				coeff[0] += val1 * val1;
			}
		}

		double result = Double.MAX_VALUE;
		double[] sol = new double[] {Double.MAX_VALUE, Double.MAX_VALUE};
		int solutions = solve_quadratic(coeff, sol);
		if (solutions == 2) {
			result = Math.max(sol[0], sol[1]);
		}
		if (solutions == 1) {
			result = sol[0];
		}

		return result;
	}

	/**
	 * add all near points (in the neighborhood) to the given point. the
	 * neighbors potential is evaluated using the recalculateAt function.
	 * 
	 * @param t
	 *        point to find near points in the neighborhood
	 * @param narrowBand
	 *        the narrow band so far
	 * @param frozenPoints
	 *        the frozenpoints so far
	 * @param elements
	 *        all elements in the room, obstacles persons etc.
	 * @param potential
	 *        the potential in the room, so far
	 */
	private void addNearPoints(Vector3D t, TreeSet<DistPoint3D> narrowBand,
			TreeSet<Vector3D> frozenPoints, CellStateFD[][][] elements,
			double[][][] potential) {
		int x = (int) (t.x);
		int y = (int) (t.y);
		int z = (int) (t.z);
		int width = potential.length;
		int height = potential[0].length;
		int depth = potential[0][0].length;
		Vector3D p;
		double dist;
		int i;
		int j;
		int k;

		for (Vector3D n : neighborPoints) {
			i = (int) n.x + x;
			j = (int) n.y + y;
			k = (int) n.z + z;

			// periodic in z
			// TODO [priority=high] [task=bugfix] [Error?] remove after configuration space experiments
			if (k >= depth) {
				k = k - depth;
			}
			if (k < 0) {
				k += depth;
			}

			if (i >= 0 && j >= 0 && k >= 0 && i < width && j < height
					&& k < depth && !(i == x && j == y && k == z)) {
				p = new Vector3D(i, j, k);
				if (elements[i][j][k] != CellStateFD.OBSTACLE
						&& !frozenPoints.contains(p)) {
					dist = recalculateAt(p, frozenPoints, potential);
					potential[i][j][k] = dist;
					DistPoint3D dp = new DistPoint3D(p, dist);
					boolean found = false;
					Iterator<DistPoint3D> nbit = narrowBand.iterator();
					// replace the point / add it in the narrowband
					while (nbit.hasNext()) {
						DistPoint3D point = nbit.next();
						if (point.equals(dp) && point.dist > dist) {
							found = true;
							nbit.remove();
							narrowBand.add(dp);
							break;
						}
					}
					if (!found) {
						narrowBand.add(dp);
					}
				}
			}
		}
	}

	/**
	 * Solve a quadratic equation given the coefficients.
	 * 
	 * @param coeff
	 *        a,b,c for cx^2+bx+a = 0
	 * @param sol
	 *        solutions (0,1 or 2) of the equation
	 * @return solution count, equals sol.length
	 */
	private int solve_quadratic(double[] coeff, double[] sol) {
		double a = coeff[2];
		double b = coeff[1];
		double c = coeff[0];

		if (a == 0 && b == 0) {
			return 0;
		}

		if (a == 0) {
			sol[0] = -c / b;
			return 1;
		}

		double det = b * b - 4 * a * c;
		if (det < 0) {
			return 0;
		} else if (det == 0) {
			sol[0] = -b / (2 * a);
			return 1;
		} else {
			sol[0] = (-b + Math.sqrt(det)) / (2 * a);
			sol[1] = (-b - Math.sqrt(det)) / (2 * a);
			return 2;
		}
	}
}

package org.vadere.simulator.models.potential.solver.gradients;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.data.cellgrid.CellGrid;

/**
 * Provides a floor gradient given the geometry of a scenario. This class uses
 * the Fast Marching algorithm (Sethian 1999) to compute a solution to
 * G(x)||grad T(x)|| = 1, the Eikonal equation. G(x) must not be equal to one,
 * like in FloorGradientProviderContinuous, however, the solution of
 * FloorGradientProviderDiscrete is only present on a discrete grid. CAREFUL:
 * this destroys (= resets to 1) the convergence rates of many higher order
 * integrators if mollification of the gradient is done improperly!
 * 
 */
public class FloorGradientProviderDiscrete implements GradientProvider {

	/**
	 * Radius of the mollifier.
	 */
	private double gradientMollifierRadius = 0.1;

	private TreeMap<Integer, CellGrid> grids = new TreeMap<Integer, CellGrid>();

	/**
	 * Ctor of FloorGradientProviderDiscrete. Runs the fast marching algorithm
	 * on the geometry given in the scenario and store the result in the
	 * potential[][] array.
	 * 
	 * @param potentialFields
	 *        pre-generated potential fields for every target in the
	 *        scenario.
	 * 
	 * @param scenarioBounds
	 * @param targets
	 */
	public FloorGradientProviderDiscrete(
			Map<Integer, CellGrid> potentialFields,
			Rectangle2D scenarioBounds, Collection<Integer> targetIds) {
		for (Integer targetID : targetIds) {
			this.grids.put(targetID, potentialFields.get(targetID));
		}
	}

	@Override
	public void gradient(double t, int currentTargetId, double[] x, double[] grad) {

		/*
		 * // MATLAB code
		 * 
		 * % clamp coordinates for index use clamp = @(r)
		 * max(1,min(size(distmap,1)-range,ceil(r))); xmin = clamp(x-range/2);
		 * xmax = clamp(x+range/2); x = clamp(x);
		 * 
		 * % weighting function [X,Y] =
		 * meshgrid(linspace(xmin(1)-x(1),xmax(1)-x(1),xmax(1)-xmin(1)+1), ...
		 * linspace(xmin(2)-x(2),xmax(2)-x(2),xmax(2)-xmin(2)+1)); sigma =
		 * range/2;
		 * 
		 * gradMap = distmap(xmin(1):xmax(1), xmin(2):xmax(2)) ... .* weight';
		 * Xgrad = X'.*gradMap; Ygrad = Y'.*gradMap;
		 * 
		 * grad = [sum(sum(Xgrad)); sum(sum(Ygrad)); 0] * cellsPerMeter /
		 * (2*(range+1)); if(norm(grad) > 1) grad = (grad) / (norm(grad)); end
		 */

		CellGrid pot = getGrid(currentTargetId);

		grad[0] = 0.0;
		grad[1] = 0.0;

		// if the target does not exist, return zero.
		if (pot == null) {
			return;
		}

		InterpolationUtil.getGradientMollified(pot, x, grad,
				gradientMollifierRadius);

		normalizeGrad(grad);

	}

	public void normalizeGrad(double[] grad) {
		double norm = Math.sqrt(grad[0] * grad[0] + grad[1] * grad[1]);
		if (norm > 1) {
			// TODO [priority=medium] [task=bugfix] [Error?]
			grad[0] /= norm;
			grad[1] /= norm;
		}
	}

	public CellGrid getGrid(int currentTargetId) {
		return this.grids.get(currentTargetId);
	}

}

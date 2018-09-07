package org.vadere.simulator.models.potential.fields;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.InterpolationUtil;

/**
 * @author Benedikt Zoennchen
 */
@FunctionalInterface
public interface IPotentialField {

    /**
     * Returns a potential at pos for the agent. This can be any potential.
     *
     * @param pos   the position for which the potential will be evaluated
     * @param agent the agent for which the potential will be evaluated
     * @return a potential at pos for the agent
     */
    double getPotential(final VPoint pos, final Agent agent);

	static IPotentialField copyAgentField(final @NotNull IPotentialField potentialField, final @NotNull Agent agent, final @NotNull VRectangle bound, final double steps) {

		final int gridWidth = (int)Math.ceil(bound.getWidth() / steps)+1;
		final int gridHeight = (int)Math.ceil(bound.getHeight() / steps)+1;
		final double[][] potentialFieldApproximation = new double[gridHeight][gridWidth];

		for(int row = 0; row < gridHeight; row++) {
			for(int col = 0; col < gridWidth; col++) {
				double x = col*steps;
				double y = row*steps;
				potentialFieldApproximation[row][col] = potentialField.getPotential(new VPoint(x, y), agent);
			}
		}

		return (pos, ped) -> {
			if(ped.equals(agent)) {
				int incX = 1;
				int incY = 1;

				int col = (int)(pos.getX() / steps);
				int row = (int)(pos.getY() / steps);

				if (col + 1 >= gridWidth) {
					incX = 0;
				}

				if (row + 1 >= gridHeight) {
					incY = 0;
				}

				VPoint gridPointCoord = new VPoint(col * steps, row * steps);
				double z1 = potentialFieldApproximation[row][col];
				double z2 = potentialFieldApproximation[row][col + incX];
				double z3 = potentialFieldApproximation[row + incY][col + incX];
				double z4 = potentialFieldApproximation[row + incY][col];

				double t = (pos.x - gridPointCoord.x) / steps;
				double u = (pos.y - gridPointCoord.y) / steps;

				return InterpolationUtil.bilinearInterpolation(z1, z2, z3, z4, t, u);
			}
			else {
				return 0.0;
			}
		};
	}
}

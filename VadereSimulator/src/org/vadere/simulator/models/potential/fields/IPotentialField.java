package org.vadere.simulator.models.potential.fields;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.models.potential.solver.EikonalSolverProvider;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.InterpolationUtil;

import java.util.List;

/**
 * A potential field for some agents: ((x,y), agent) -> potential.
 *
 * @author Benedikt Zoennchen
 */
@FunctionalInterface
public interface IPotentialField {

    /**
     * Returns a potential at pos for the agent. This can be any potential:
     * ((x,y), agent) -> potential
     *
     * @param pos   the position for which the potential will be evaluated
     * @param agent the agent for which the potential will be evaluated
     * @return a potential at pos for the agent
     */
    double getPotential(final IPoint pos, final Agent agent);

    Logger logger = Logger.getLogger(IPotentialField.class);

    /**
     * Factory method to construct an EikonalSolver for agents of target defined by targetShapes and targetId.
     * This method will also generate the underlying mesh/grid which discretise the spacial domain.
     *
     * @param domain                the spatial domain
     * @param targetId              the
     * @param targetShapes          the area where T = 0
     * @param attributesPedestrian  pedestrian configuration
     * @param attributesPotential   potential field configuration (dynamic or static, parameters and so on...)
	 * @return an EikonalSolver for a specific target
     */
    static EikonalSolver create(
            final Domain domain,
            final int targetId,
            final List<VShape> targetShapes,
            final AttributesAgent attributesPedestrian,
            final AttributesFloorField attributesPotential) {
		logger.debug("create EikonalSolver");

		// retrieve EikonalSolverProvider from context object.
		EikonalSolverProvider provider = VadereContext.getCtx(domain.getTopography()).getEikonalSolverProvider();

        return provider.provide(domain, targetId, targetShapes, attributesPedestrian, attributesPotential);
    }

	static EikonalSolver create(final Domain domain,
	                            final Topography topography,
	                            final int targetId,
	                            final List<VShape> targetShapes,
	                            final AttributesAgent attributesPedestrian,
	                            final AttributesFloorField attributesPotential)
	{
		EikonalSolverProvider provider = VadereContext.getCtx(topography).getEikonalSolverProvider();
		return provider.provide(domain, targetId, targetShapes, attributesPedestrian, attributesPotential);
	}


	static IPotentialField copyAgentField(
			final @NotNull IPotentialField potentialField,
			final @NotNull Agent agent,
			final @NotNull VRectangle bound,
			final double steps) {

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

				int col = (int)((pos.getX() - bound.getMinX()) / steps);
				int row = (int)((pos.getY() - bound.getMinY()) / steps);

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

				double t = (pos.getX() - gridPointCoord.x) / steps;
				double u = (pos.getY() - gridPointCoord.y) / steps;

				return InterpolationUtil.bilinearInterpolation(z1, z2, z3, z4, t, u);
			}
			else {
				return 0.0;
			}
		};
	}
}

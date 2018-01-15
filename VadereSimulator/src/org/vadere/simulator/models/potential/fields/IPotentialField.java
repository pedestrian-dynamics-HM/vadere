package org.vadere.simulator.models.potential.fields;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.models.potential.timeCostFunction.TimeCostFunctionFactory;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.EikonalSolverType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.CellState;
import org.vadere.util.potential.FloorDiscretizer;
import org.vadere.util.potential.PathFindingTag;
import org.vadere.util.potential.calculators.EikonalSolver;
import org.vadere.util.potential.calculators.PotentialFieldCalculatorNone;
import org.vadere.util.potential.calculators.cartesian.EikonalSolverFIM;
import org.vadere.util.potential.calculators.cartesian.EikonalSolverFMM;
import org.vadere.util.potential.calculators.cartesian.EikonalSolverFSM;
import org.vadere.util.potential.timecost.ITimeCostFunction;

import java.awt.geom.Rectangle2D;
import java.util.List;

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


    static Logger logger = LogManager.getLogger(IPotentialField.class);

    static EikonalSolver create(
            final Topography topography,
            final int targetId,
            final List<VShape> targetShapes,
            final AttributesAgent attributesPedestrian,
            final AttributesFloorField attributesPotential) {

        EikonalSolverType createMethod = attributesPotential.getCreateMethod();

        Rectangle2D bounds = topography.getBounds();
        CellGrid cellGrid = new CellGrid(bounds.getWidth(), bounds.getHeight(),
                attributesPotential.getPotentialFieldResolution(), new CellState());

        if (createMethod != EikonalSolverType.NONE) {
            for (VShape shape : targetShapes) {
                FloorDiscretizer.setGridValuesForShapeCentered(cellGrid, shape,
                        new CellState(0.0, PathFindingTag.Target));
            }

            for (Obstacle obstacle : topography.getObstacles()) {
                FloorDiscretizer.setGridValuesForShapeCentered(cellGrid, obstacle.getShape(),
                        new CellState(Double.MAX_VALUE, PathFindingTag.Obstacle));
            }
        }

        boolean isHighAccuracyFM = createMethod.isHighAccuracy();

        ITimeCostFunction timeCost = TimeCostFunctionFactory.create(
                attributesPotential.getTimeCostAttributes(),
                attributesPedestrian,
                topography,
                targetId, 1.0 / cellGrid.getResolution());

		/* copy the static grid */
        EikonalSolver eikonalSolver;
        switch (createMethod) {
            case NONE:
                eikonalSolver = new PotentialFieldCalculatorNone();
                break;
            case FAST_ITERATIVE_METHOD:
                eikonalSolver = new EikonalSolverFIM(cellGrid, targetShapes, isHighAccuracyFM, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
                break;
            case FAST_SWEEPING_METHOD:
                eikonalSolver = new EikonalSolverFSM(cellGrid, targetShapes, isHighAccuracyFM, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
                break;
            default:
                eikonalSolver = new EikonalSolverFMM(cellGrid, targetShapes, isHighAccuracyFM, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
        }

        long ms = System.currentTimeMillis();
        eikonalSolver.initialize();
        logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));
        return eikonalSolver;
    }
}

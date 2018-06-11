package org.vadere.simulator.models.potential.fields;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.types.EikonalSolverType;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.CellState;
import org.vadere.util.potential.FloorDiscretizer;
import org.vadere.util.potential.PathFindingTag;
import org.vadere.util.potential.calculators.EikonalSolver;
import org.vadere.util.potential.calculators.cartesian.EikonalSolverFIM;
import org.vadere.util.potential.calculators.cartesian.EikonalSolverFMM;
import org.vadere.util.potential.calculators.cartesian.EikonalSolverFSM;
import org.vadere.util.potential.calculators.cartesian.EikonalSolverSFMM;
import org.vadere.util.potential.calculators.PotentialFieldCalculatorNone;
import org.vadere.util.potential.timecost.UnitTimeCostFunction;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public class ObstacleDistancePotential implements IPotentialField {

	private static Logger logger = LogManager.getLogger(ObstacleDistancePotential.class);
	private final EikonalSolver eikonalSolver;

	public ObstacleDistancePotential(@NotNull final Collection<VShape> obstacles,
	                                 @NotNull final VRectangle bounds,
	                                 @NotNull final AttributesFloorField attributesFloorField) {


		CellGrid cellGrid = new CellGrid(bounds.getWidth(), bounds.getHeight(), attributesFloorField.getPotentialFieldResolution(), new CellState());


		for (VShape shape : obstacles) {
			FloorDiscretizer.setGridValuesForShapeCentered(cellGrid, shape, new CellState(0.0, PathFindingTag.Target));
		}

		boolean isHighAccuracyFM = attributesFloorField.getCreateMethod().isHighAccuracy();

		/**
		 * The distance function returns values < 0 if the point is inside the domain,
		 * i.e. outside of any obstacle and values > 0 if the point lies inside an obstacle.
		 */
		IDistanceFunction distFunc = new DistanceFunctionTarget(cellGrid, obstacles);

		/* copy the static grid */
		switch (attributesFloorField.getCreateMethod()) {
			case NONE:
				eikonalSolver = new PotentialFieldCalculatorNone();
				break;
			case FAST_ITERATIVE_METHOD:
				eikonalSolver = new EikonalSolverFIM(cellGrid, distFunc, isHighAccuracyFM, new UnitTimeCostFunction(), attributesFloorField.getObstacleGridPenalty(), attributesFloorField.getTargetAttractionStrength());
				break;
			case FAST_SWEEPING_METHOD:
				eikonalSolver = new EikonalSolverFSM(cellGrid, distFunc, isHighAccuracyFM, new UnitTimeCostFunction(), attributesFloorField.getObstacleGridPenalty(), attributesFloorField.getTargetAttractionStrength());
				break;
			default:
				eikonalSolver = new EikonalSolverFMM(cellGrid, distFunc, isHighAccuracyFM, new UnitTimeCostFunction(), attributesFloorField.getObstacleGridPenalty(), attributesFloorField.getTargetAttractionStrength());
		}

		long ms = System.currentTimeMillis();
		eikonalSolver.initialize();
		logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));
	}

	@Override
	public double getPotential(@NotNull IPoint pos, @Nullable Agent agent) {
		return eikonalSolver.getPotential(pos);
	}
}

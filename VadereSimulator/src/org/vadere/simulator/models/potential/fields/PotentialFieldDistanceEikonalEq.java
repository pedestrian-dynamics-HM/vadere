package org.vadere.simulator.models.potential.fields;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.DistanceFunctionTarget;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.FloorDiscretizer;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFIM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFSM;
import org.vadere.simulator.models.potential.solver.calculators.PotentialFieldCalculatorNone;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.util.math.IDistanceFunction;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 *
 * PotentialFieldDistanceEikonalEq computes the nearest distnace to any obstacle by computing
 * the distance at certain discrete points lying on an Cartesian grid. Values inbetween are
 * bilinear interpolated. To compute the distance at these grid points the eikonal equation is
 * used by choosing obstacles to be the destination area of the propageting wave front.
 */
public class PotentialFieldDistanceEikonalEq implements IPotentialField {

	private static Logger logger = LogManager.getLogger(PotentialFieldDistanceEikonalEq.class);
	private final EikonalSolver eikonalSolver;

	public PotentialFieldDistanceEikonalEq(@NotNull final Collection<VShape> obstacles,
										   @NotNull final VRectangle bounds,
										   @NotNull final AttributesFloorField attributesFloorField) {
		CellGrid cellGrid = new CellGrid(bounds.getWidth(),
				bounds.getHeight(), attributesFloorField.getPotentialFieldResolution(), new CellState(), bounds.getMinX(), bounds.getMinY());


		for (VShape shape : obstacles) {
			FloorDiscretizer.setGridValuesForShape(cellGrid, shape,
					new CellState(0.0, PathFindingTag.Target));
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
		// unknownPenalty = 0.0 since there will be no unknowns such as values at obstacles
		// the fmm can cause an topographyError mostly an underestimation of 20% near the source which are exactly the points we are interested
		return eikonalSolver.getPotential(pos, 0.0, 1.2);
	}

	public EikonalSolver getEikonalSolver() {
		return eikonalSolver;
	}
}

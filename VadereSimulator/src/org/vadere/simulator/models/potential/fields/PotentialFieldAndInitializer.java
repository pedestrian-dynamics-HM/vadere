package org.vadere.simulator.models.potential.fields;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.models.potential.timeCostFunction.TimeCostFunctionFactory;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.EikonalSolverType;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.CellState;
import org.vadere.util.potential.FloorDiscretizer;
import org.vadere.util.potential.PathFindingTag;
import org.vadere.util.potential.calculators.*;
import org.vadere.util.potential.timecost.ITimeCostFunction;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A tupel (PotentialFieldCalculator, AttributesFloorField). Furthermore this class
 * contains the factory method to create a PotentialFieldAndInitializer object.
 *
 */
class PotentialFieldAndInitializer {
	protected EikonalSolver eikonalSolver;
	protected AttributesFloorField attributesFloorField;
	private static Logger logger = LogManager.getLogger(PotentialFieldAndInitializer.class);

	protected PotentialFieldAndInitializer(
			final EikonalSolver eikonalSolver,
			final AttributesFloorField attributesFloorField) {
		this.eikonalSolver = eikonalSolver;
		this.attributesFloorField = attributesFloorField;
	}

	protected static PotentialFieldAndInitializer create(
			final Topography topography,
			final int targetId,
			final List<VShape> targetShapes,
			final AttributesAgent attributesPedestrian,
			final AttributesFloorField attributesPotential) {

		EikonalSolverType createMethod = attributesPotential.getCreateMethod();

		Rectangle2D bounds = topography.getBounds();
		CellGrid cellGrid = new CellGrid(bounds.getWidth(), bounds.getHeight(),
				attributesPotential.getPotentialFieldResolution(), new CellState(), bounds.getMinX(), bounds.getMinY());

		if (createMethod != EikonalSolverType.NONE) {
			for (VShape shape : targetShapes) {
				FloorDiscretizer.setGridValuesForShape(cellGrid, shape,
						new CellState(0.0, PathFindingTag.Target));
			}

			for (Obstacle obstacle : topography.getObstacles()) {
				FloorDiscretizer.setGridValuesForShape(cellGrid, obstacle.getShape(),
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
				eikonalSolver = new EikonalSolverFIM(cellGrid, targetShapes, timeCost);
				break;
			case FAST_SWEEPING_METHOD:
				eikonalSolver = new EikonalSolverFSM(cellGrid, targetShapes, timeCost);
				break;
			default:
				eikonalSolver = new EikonalSolverSFMM(cellGrid, targetShapes, isHighAccuracyFM, timeCost);
		}

		long ms = System.currentTimeMillis();
		eikonalSolver.initialize();
		logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));

		return new PotentialFieldAndInitializer(eikonalSolver, attributesPotential);
	}
}

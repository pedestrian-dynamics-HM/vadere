package org.vadere.simulator.models.potential.solver;


import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFIM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFSM;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.FloorDiscretizer;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.DistanceFunctionTarget;
import org.vadere.util.math.IDistanceFunction;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestEikonalSolver {

	private Logger logger = Logger.getLogger(TestEikonalSolver.class);

	private double gridWidth = 20;
	private double gridHeight = 20;

	private double potentialFieldResolution = 0.1;
	private double obstacleGridPenalty = 0.1;
	private double targetAttractionStrength = 1.0;

	private double dx = 0.05;
	private double dy = 0.05;
	private CellGrid cellGrid;
	private double maxError = 0.5;

	private VRectangle targetShape = new VRectangle(7.5, 7.5, 5, 5);

	private VPoint[] targetPoints = new VPoint[]{ new VPoint(10,10)};

	private List<VShape> targetShapes = new ArrayList<>();

	private IDistanceFunction distFunc;

	private double unknownPenalty = 0.1;

	private double weight = 1.0;

	@Before
	public void setUp() throws Exception {
		cellGrid = new CellGrid(gridWidth, gridHeight, potentialFieldResolution, new CellState(Double.MAX_VALUE, PathFindingTag.Undefined));
		FloorDiscretizer.setGridValuesForShape(cellGrid, targetShape, new CellState(0.0, PathFindingTag.Target));
		targetShapes.add(targetShape);
		distFunc = new DistanceFunctionTarget(cellGrid, targetShapes);
	}

	@Test
	public void TestFMM() {
		double ms = System.currentTimeMillis();
		EikonalSolver eikonalSolverFMM = new EikonalSolverFMM(cellGrid,
				distFunc, false, new UnitTimeCostFunction(), unknownPenalty, weight);
		eikonalSolverFMM.solve();
		testMaxError(eikonalSolverFMM);
		double runtimeInMs = System.currentTimeMillis() - ms;
		logger.info("FMM (not high accuracy) required " + runtimeInMs + "[ms]");
	}

	@Test
	public void TestFIM() {
		double ms = System.currentTimeMillis();
		EikonalSolver eikonalSolverFIM = new EikonalSolverFIM(cellGrid,
				distFunc, false, new UnitTimeCostFunction(), unknownPenalty, weight);
		eikonalSolverFIM.solve();
		testMaxError(eikonalSolverFIM);
		double runtimeInMs = System.currentTimeMillis() - ms;
		logger.info("FIM (not high accuracy) required " + runtimeInMs + "[ms]");
	}

	@Test
	public void TestFSM() {
		double ms = System.currentTimeMillis();
		EikonalSolver eikonalSolverFSM = new EikonalSolverFSM(cellGrid,
				distFunc, false, new UnitTimeCostFunction(), unknownPenalty, weight);
		eikonalSolverFSM.solve();
		testMaxError(eikonalSolverFSM);
		double runtimeInMs = System.currentTimeMillis() - ms;
		logger.info("FSM (not high accuracy) required " + runtimeInMs + "[ms]");
	}

	private void testMaxError(EikonalSolver eikonalSolver) {
		double max = Double.MIN_VALUE;
		for(double x = 0; x < gridWidth; x += dx) {
			for(double y = 0; y < gridHeight; y += dy) {
				double distance = Math.max(0, targetShape.distance(new VPoint(x, y)));
				double travellingTime = eikonalSolver.getPotential(new VPoint(x, y), obstacleGridPenalty, targetAttractionStrength);
				max = Math.max(max, Math.abs(travellingTime-distance));
			}
		}

		logger.info(eikonalSolver + " max error = " + max);
		assertTrue(max <= maxError);
	}

}

package org.vadere.util.potential;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.calculators.EikonalSolver;
import org.vadere.util.potential.calculators.EikonalSolverFIM;
import org.vadere.util.potential.calculators.EikonalSolverFMM;
import org.vadere.util.potential.calculators.EikonalSolverFSM;
import org.vadere.util.potential.timecost.UnitTimeCostFunction;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestEikonalSolver {

	private Logger logger = LogManager.getLogger(TestEikonalSolver.class);

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

	@Before
	public void setUp() throws Exception {
		cellGrid = new CellGrid(gridWidth, gridHeight, potentialFieldResolution, new CellState(Double.MAX_VALUE, PathFindingTag.Undefined));
		FloorDiscretizer.setGridValuesForShape(cellGrid, targetShape, new CellState(0.0, PathFindingTag.Target));
		targetShapes.add(targetShape);
	}

	@Test
	public void TestFMM() {
		double ms = System.currentTimeMillis();
		EikonalSolver eikonalSolverFMM = new EikonalSolverFMM(cellGrid, targetShapes, false, new UnitTimeCostFunction());
		eikonalSolverFMM.initialize();
		testMaxError(eikonalSolverFMM);
		double runtimeInMs = System.currentTimeMillis() - ms;
		logger.info("FMM (not high accuracy) required " + runtimeInMs + "[ms]");
	}

	@Test
	public void TestFIM() {
		double ms = System.currentTimeMillis();
		EikonalSolver eikonalSolverFIM = new EikonalSolverFIM(cellGrid, targetShapes, new UnitTimeCostFunction());
		eikonalSolverFIM.initialize();
		testMaxError(eikonalSolverFIM);
		double runtimeInMs = System.currentTimeMillis() - ms;
		logger.info("FIM (not high accuracy) required " + runtimeInMs + "[ms]");
	}

	@Test
	public void TestFSM() {
		double ms = System.currentTimeMillis();
		EikonalSolver eikonalSolverFSM = new EikonalSolverFSM(cellGrid, targetShapes, new UnitTimeCostFunction());
		eikonalSolverFSM.initialize();
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

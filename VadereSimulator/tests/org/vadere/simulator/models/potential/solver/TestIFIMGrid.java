package org.vadere.simulator.models.potential.solver;


import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFIM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverIFIM;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;

/**
 * @author Benedikt Zoennchen
 */
public class TestIFIMGrid {

	private Logger logger = Logger.getLogger(TestIFIMGrid.class);

	private double gridWidth = 1.8;
	private double gridHeight = 1.8;

	private double potentialFieldResolution = 0.01;
	private double obstacleGridPenalty = 0.1;
	private double targetAttractionStrength = 1.0;

	private double dx = 0.05;
	private double dy = 0.05;
	private CellGrid cellGrid;
	private double maxError = 0.5;

	private IDistanceFunction distFunc;

	private double unknownPenalty = 0.1;

	private double weight = 1.0;

	private ITimeCostFunction timeCostFunction;

	@Before
	public void setUp() throws Exception {
		cellGrid = new CellGrid(gridWidth, gridHeight, potentialFieldResolution, new CellState(Double.MAX_VALUE, PathFindingTag.Undefined), -0.9, -0.9);
		//FloorDiscretizer.setGridValuesForShape(cellGrid, targetShape, new CellState(0.0, PathFindingTag.Target));
		cellGrid.setValue(new Point((cellGrid.getNumPointsX() / 2), (cellGrid.getNumPointsY()) / 2), new CellState(0.0, PathFindingTag.Target));
		distFunc = p -> -p.distanceToOrigin();
		timeCostFunction = p -> 1.0 / (0.8 * Math.sin(2 * Math.PI * p.getX()) * Math.sin(2 * Math.PI * p.getY()) + 1.0);
	}

	@Test
	public void TestFMM() {
		double ms = System.currentTimeMillis();
		EikonalSolver eikonalSolverFMM = new EikonalSolverFMM(cellGrid,
				distFunc, false, timeCostFunction, unknownPenalty, weight);
		eikonalSolverFMM.solve();
		double runtimeInMs = System.currentTimeMillis() - ms;
		logger.info("FMM (not high accuracy) required " + runtimeInMs + "[ms]");
		System.out.print("[");
		for(int i = 0; i < cellGrid.getNumPointsY(); i++) {
			System.out.print("[");
			for(int j = 0; j < cellGrid.getNumPointsX(); j++) {
				System.out.print(cellGrid.getValue(i,j).potential);
				if(j < cellGrid.getNumPointsX()-1) {
					System.out.print(",");
				}
			}
			System.out.print("]");
			if(i < cellGrid.getNumPointsY()-1) {
				System.out.println(",");
			}
		}
		System.out.print("]");
	}

	@Test
	public void TestFIM() {
		double ms = System.currentTimeMillis();
		EikonalSolver eikonalSolverFIM = new EikonalSolverFIM(cellGrid,
				distFunc, false, timeCostFunction, unknownPenalty, weight);
		eikonalSolverFIM.solve();
		double runtimeInMs = System.currentTimeMillis() - ms;
		logger.info("FIM (not high accuracy) required " + runtimeInMs + "[ms]");
	}

	@Test
	public void TestIFIM() {
		double ms = System.currentTimeMillis();
		EikonalSolver eikonalSolverFIM = new EikonalSolverIFIM(cellGrid,
				distFunc, timeCostFunction, unknownPenalty, weight);
		eikonalSolverFIM.solve();
		double runtimeInMs = System.currentTimeMillis() - ms;
		logger.info("IFIM (not high accuracy) required " + runtimeInMs + "[ms]");

		ms = System.currentTimeMillis();
		eikonalSolverFIM.solve();
		runtimeInMs = System.currentTimeMillis() - ms;
		logger.info("IFIM (not high accuracy) required " + runtimeInMs + "[ms]");

		ms = System.currentTimeMillis();
		eikonalSolverFIM.solve();
		runtimeInMs = System.currentTimeMillis() - ms;
		logger.info("IFIM (not high accuracy) required " + runtimeInMs + "[ms]");

		ms = System.currentTimeMillis();
		eikonalSolverFIM.solve();
		runtimeInMs = System.currentTimeMillis() - ms;
		logger.info("IFIM (not high accuracy) required " + runtimeInMs + "[ms]");

		System.out.print("[");
		for(int i = 0; i < cellGrid.getNumPointsY(); i++) {
			System.out.print("[");
			for(int j = 0; j < cellGrid.getNumPointsX(); j++) {
				System.out.print(cellGrid.getValue(i,j).potential);
				if(j < cellGrid.getNumPointsX()-1) {
					System.out.print(",");
				}
			}
			System.out.print("]");
			if(i < cellGrid.getNumPointsY()-1) {
				System.out.println(",");
			}
		}
		System.out.print("]");
	}

	/*@Test
	public void TestFSM() {
		double ms = System.currentTimeMillis();
		EikonalSolver eikonalSolverFSM = new EikonalSolverFSM(cellGrid,
				distFunc, false, timeCostFunction, unknownPenalty, weight);
		eikonalSolverFSM.solve();
		testMaxError(eikonalSolverFSM);
		double runtimeInMs = System.currentTimeMillis() - ms;
		logger.info("FSM (not high accuracy) required " + runtimeInMs + "[ms]");
	}*/


}

package org.vadere.util.potential;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.calculators.EikonalSolverFMM;
import org.vadere.util.potential.calculators.EikonalSolverSFMM;
import org.vadere.util.potential.timecost.UnitTimeCostFunction;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Benedikt Zoennchen
 *
 * Comparing the performance of the fast marching method FMM and the simpliefied fast marchning mehtod SFMM.
 */
public class PerformanceSFMM {

	private static final List<VShape> obstacles = Arrays.asList(
			new VRectangle(16, 39.8, 5.6, 40.3),
			new VRectangle(21.5, 73.1, 45.8, 7.0),
			new VRectangle(60.9, 38.4, 6.4, 34.7));

	private static final List<VShape> targets = Arrays.asList(new VRectangle(31.3, 86.4, 30.0, 10.0));

	private static final VRectangle bounds = new VRectangle(0, 0, 100, 100);

	private static final double resolution = 0.1;

	@State(Scope.Thread)
	public static class StateCellGrid {
		public CellGrid cellGrid;

		@Setup(Level.Invocation)
		public void doSetup() {

			cellGrid = new CellGrid(bounds.getWidth(), bounds.getHeight(), resolution, new CellState());
			for (VShape shape : targets) {
				FloorDiscretizer.setGridValuesForShape(cellGrid, shape, new CellState(0.0, PathFindingTag.Target));
			}

			for (VShape obstacle : obstacles) {
				FloorDiscretizer.setGridValuesForShape(cellGrid, obstacle, new CellState(Double.MAX_VALUE, PathFindingTag.Obstacle));
			}
		}
	}

	@Benchmark
	@BenchmarkMode(Mode.SingleShotTime) @OutputTimeUnit(TimeUnit.MILLISECONDS)
	public double testFMM(PerformanceSFMM.StateCellGrid stateCellGrid) {
		EikonalSolverFMM ffm = new EikonalSolverFMM(stateCellGrid.cellGrid, targets, true, new UnitTimeCostFunction());
		ffm.initialize();
		return stateCellGrid.cellGrid.getValue(0, 0).potential;
	}

	@Benchmark
	@BenchmarkMode(Mode.SingleShotTime) @OutputTimeUnit(TimeUnit.MILLISECONDS)
	public double testSFMM(PerformanceSFMM.StateCellGrid stateCellGrid) {
		EikonalSolverSFMM ffm = new EikonalSolverSFMM(stateCellGrid.cellGrid, targets, true, new UnitTimeCostFunction());
		ffm.initialize();
		return stateCellGrid.cellGrid.getValue(0, 0).potential;
	}
}

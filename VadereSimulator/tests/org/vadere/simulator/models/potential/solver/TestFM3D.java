package org.vadere.simulator.models.potential.solver;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vadere.util.data.cellgrid.CellStateFD;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.io.GeometryPrinter;
import org.vadere.util.io.IOUtils;
import org.vadere.simulator.models.potential.solver.calculators.PotentialFieldCalculatorFastMarching3D;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction3D;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction3D;

/**
 * Tests the {@link PotentialFieldCalculatorFastMarching3D} class. Note that the
 * tests are not automatic, this class only generates files that need to be
 * checked visually with the MATLAB files in the matlab folder.
 * 
 */
public class TestFM3D {

	private static final int width = 20;
	private static final int height = 20;
	private static final int depth = 20;
	private static ITimeCostFunction3D timecost;
	private static PotentialFieldCalculatorFastMarching3D solver;

	@BeforeClass
	public static void setUpBeforeClass() {
		timecost = new UnitTimeCostFunction3D();
		solver = new PotentialFieldCalculatorFastMarching3D(timecost);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {}

	/**
	 * Test method for
	 * {@link org.vadere.simulator.models.potential.solver.calculators.PotentialFieldCalculatorFastMarching3D#recalculate(double[][][], org.vadere.util.potential.CellStateFD[][][], java.util.List)}
	 * .
	 */
	@Test
	public void testRecalculate() {
		double[][][] potential = new double[width][height][depth];
		CellStateFD[][][] elements = new CellStateFD[width][height][depth];
		List<Vector3D> targets = new LinkedList<>();

		targets.add(new Vector3D(width / 2, height / 2, 0));

		fill(elements, CellStateFD.EMPTY);

		for (Vector3D target : targets) {
			elements[(int) target.x][(int) target.y][(int) target.z] = CellStateFD.TARGET;
		}

		potential = solver.recalculate(potential, elements, targets);

		String str = GeometryPrinter.grid2string(potential);
		try {
			IOUtils.printDataFile(Paths.get("output", "test_FM_3D"), str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Fills the given 3d array with the given state
	 * 
	 * @param elements
	 * @param state
	 */
	private void fill(CellStateFD[][][] elements, CellStateFD state) {
		for (int x = 0; x < elements.length; x++) {
			for (int y = 0; y < elements.length; y++) {
				for (int z = 0; z < elements.length; z++) {
					elements[x][y][z] = state;
				}
			}
		}
	}

}

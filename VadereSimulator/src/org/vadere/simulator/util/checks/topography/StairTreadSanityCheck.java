package org.vadere.simulator.util.checks.topography;

import org.vadere.simulator.util.ScenarioCheckerMessage;
import org.vadere.simulator.util.ScenarioCheckerReason;
import org.vadere.simulator.util.checks.AbstractScenarioCheck;
import org.vadere.simulator.util.checks.TopographyCheckerTest;
import org.vadere.state.scenario.Stairs;
import org.vadere.state.scenario.Topography;

import java.util.PriorityQueue;

public class StairTreadSanityCheck extends AbstractScenarioCheck implements TopographyCheckerTest {
	@Override
	public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {
		PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
		topography.getStairs().forEach(stairs -> {
			double treadDepth = stairs.getTreadDepth();
			if (treadDepth < Stairs.MIN_TREAD_DEPTH || treadDepth > Stairs.MAX_TREAD_DEPTH) {
				ret.add(msgBuilder
						.topographyWarning()
						.target(stairs)
						.reason(ScenarioCheckerReason.STAIRS_TREAD_DIM_WRONG
								, "(" + Stairs.MIN_TREAD_DEPTH + "m  &lt; treadDepth  &gt; " + Stairs.MAX_TREAD_DEPTH +
										"m) current treadDepth is: " + String.format("%.3fm", stairs.getTreadDepth()))
						.build()
				);
			}
		});

		return ret;
	}
}

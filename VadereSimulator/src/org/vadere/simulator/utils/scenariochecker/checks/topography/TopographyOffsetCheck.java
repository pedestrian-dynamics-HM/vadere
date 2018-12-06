package org.vadere.simulator.utils.scenariochecker.checks.topography;

import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.simulator.utils.scenariochecker.checks.TopographyCheckerTest;
import org.vadere.state.scenario.Topography;

import java.util.PriorityQueue;

public class TopographyOffsetCheck extends AbstractScenarioCheck implements TopographyCheckerTest {
	@Override
	public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {
		PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
		if (topography.getBounds().x != 0.0 || topography.getBounds().y != 0.0){
			ret.add(msgBuilder.topographyWarning().reason(ScenarioCheckerReason.TOPOGRAPHY_OFFSET).build());
		}
		return ret;
	}
}

package org.vadere.simulator.utils.scenariochecker.checks.topography;

import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.simulator.utils.scenariochecker.checks.TopographyCheckerTest;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

public class UniqueSourceIdCheck extends AbstractScenarioCheck implements TopographyCheckerTest {
	@Override
	public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {
		PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
		Set<Integer> sourceId = new HashSet<>();

		for (Source s : topography.getSources()) {
			if (!sourceId.add(s.getId())) {
				ret.add(msgBuilder.topographyWarning()
						.target(s)
						.reason(ScenarioCheckerReason.SOURCE_ID_NOT_UNIQUE)
						.build());
			}
		}
		return ret;
	}
}

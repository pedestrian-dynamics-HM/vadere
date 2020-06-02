package org.vadere.simulator.utils.scenariochecker.checks.topography;

import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.simulator.utils.scenariochecker.checks.TopographyCheckerTest;
import org.vadere.state.scenario.Topography;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

public class UnusedTargetsCheck extends AbstractScenarioCheck implements TopographyCheckerTest {
	@Override
	public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {
		PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
		Set<Integer> usedTargetIds = new HashSet<>();
		topography.getSources()
				.forEach(s -> usedTargetIds.addAll(s.getAttributes().getTargetIds()));
		topography.getTargetChangers()
				.forEach(s -> usedTargetIds.addAll(s.getAttributes().getNextTarget()));

		topography.getTargets().forEach(t -> {
			if (!usedTargetIds.contains(t.getId())) {
				ret.add(msgBuilder
						.topographyWarning()
						.reason(ScenarioCheckerReason.TARGET_UNUSED)
						.target(t)
						.build());
			}
		});

		return ret;
	}
}

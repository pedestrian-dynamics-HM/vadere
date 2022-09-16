package org.vadere.simulator.utils.scenariochecker.checks.topography;

import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.simulator.utils.scenariochecker.checks.TopographyCheckerTest;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidTargetsInSourceCheck extends AbstractScenarioCheck implements TopographyCheckerTest {
	@Override
	public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {
		PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
		Set<Integer> targetIds = topography.getTargets().stream()
				.map(Target::getId)
				.collect(Collectors.toSet());

		for (Source s : topography.getSources()) {
			if (s.getAttributes().getTargetIds().size() == 0) {
				if (s.getAttributes().getSpawnerAttributes().getEventElementCount() == 0) {
					ret.add(msgBuilder
							.topographyWarning()
							.target(s)
							.reason(ScenarioCheckerReason.SOURCE_NO_TARGET_ID_NO_SPAWN)
							.build());
				} else {
					ret.add(msgBuilder.topographyError()
							.target(s)
							.reason(ScenarioCheckerReason.SOURCE_NO_TARGET_ID_SET)
							.build());
				}
			} else {
				List<String> notFoundTargetIds = s.getAttributes().getTargetIds().stream()
						.filter(tId -> !targetIds.contains(tId))
						.map(tId -> Integer.toString(tId))
						.collect(Collectors.toList());
				if (notFoundTargetIds.size() > 0) {
					StringBuilder sj = new StringBuilder();
					sj.append("[");
					notFoundTargetIds.forEach(i -> sj.append(i).append(", "));
					sj.setLength(sj.length() - 2);
					sj.append("]");
					ret.add(msgBuilder.topographyError()
							.target(s)
							.reason(ScenarioCheckerReason.SOURCE_TARGET_ID_NOT_FOUND, sj.toString())
							.build());
				}
			}
		}

		return ret;
	}
}

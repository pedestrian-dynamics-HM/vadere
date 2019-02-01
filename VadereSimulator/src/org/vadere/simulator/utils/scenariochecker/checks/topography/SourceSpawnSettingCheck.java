package org.vadere.simulator.utils.scenariochecker.checks.topography;

import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.simulator.utils.scenariochecker.checks.TopographyCheckerTest;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;

import java.util.List;
import java.util.PriorityQueue;

public class SourceSpawnSettingCheck extends AbstractScenarioCheck implements TopographyCheckerTest {
	@Override
	public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {
		List<Source> sourceList = topography.getSources();

		for (Source source : sourceList) {
			AttributesSource attr = source.getAttributes();

			if (attr.isSpawnAtRandomPositions() && !attr.isUseFreeSpaceOnly()){
				messages.add(msgBuilder.topographyWarning().target(source)
						.reason(ScenarioCheckerReason.SOURCE_SPAWN_RND_POS_NOT_FREE_SPACE).build());
			} else if (!attr.isUseFreeSpaceOnly()){
				messages.add(msgBuilder.topographyWarning().target(source)
						.reason(ScenarioCheckerReason.SOURCE_SPAWN_USE_NOT_FREE_SPACE).build());
			}

		}

		return messages;
	}
}

package org.vadere.simulator.utils.scenariochecker.checks.topography;

import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.simulator.utils.scenariochecker.checks.TopographyCheckerTest;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.PriorityQueue;

public class PedestrianSpeedSetupCheck extends AbstractScenarioCheck implements TopographyCheckerTest {
	@Override
	public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {
		PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
		topography.getPedestrianDynamicElements().getInitialElements().forEach(p -> {
			double speedMean = p.getSpeedDistributionMean();
			if (speedMean < p.getAttributes().getMinimumSpeed() || speedMean > p.getAttributes().getMaximumSpeed()) {
				ret.add(msgBuilder
						.topographyError()
						.target(p)
						.reason(ScenarioCheckerReason.PEDESTRIAN_SPEED_SETUP,
								String.format("[min: %.2f, max: %.2f, mean: %.2f]",
										p.getAttributes().getMinimumSpeed(),
										p.getAttributes().getMaximumSpeed(),
										speedMean))
						.build());
			}
			if (p.getAttributes().getMinimumSpeed() > Pedestrian.PEDESTRIAN_MAX_SPEED_METER_PER_SECOND || p.getAttributes().getMaximumSpeed() > Pedestrian.PEDESTRIAN_MAX_SPEED_METER_PER_SECOND) {
				ret.add(msgBuilder
						.topographyWarning()
						.target(p)
						.reason(ScenarioCheckerReason.PEDESTRIAN_SPEED_NOT_LOGICAL,
								String.format("[max: %.2f min: %.2f threshold: %.2f]", p.getAttributes().getMinimumSpeed(),
										p.getAttributes().getMaximumSpeed(), Pedestrian.PEDESTRIAN_MAX_SPEED_METER_PER_SECOND))
						.build());
			}
			if (p.getAttributes().getMinimumSpeed() < 0 || p.getAttributes().getMaximumSpeed() < 0) {
				ret.add(msgBuilder
						.topographyError()
						.target(p)
						.reason(ScenarioCheckerReason.PEDESTRIAN_SPEED_NEGATIVE)
						.build());
			}
		});

		return ret;
	}
}

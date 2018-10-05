package org.vadere.simulator.util.checks.topography;

import org.vadere.simulator.util.ScenarioCheckerMessage;
import org.vadere.simulator.util.ScenarioCheckerReason;
import org.vadere.simulator.util.checks.AbstractScenarioCheck;
import org.vadere.simulator.util.checks.TopographyCheckerTest;
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
								"(" + p.getAttributes().getMinimumSpeed()
										+ "  &lt; treadDepth  &lt; "
										+ p.getAttributes().getMaximumSpeed() +
										") current SpeedDistributionMean is: " + String.format("%.2f", speedMean))
						.build());
			}
			if (p.getAttributes().getMinimumSpeed() > Pedestrian.HUMAN_MAX_SPEED || p.getAttributes().getMaximumSpeed() > Pedestrian.HUMAN_MAX_SPEED) {
				ret.add(msgBuilder
						.topographyWarning()
						.target(p)
						.reason(ScenarioCheckerReason.PEDESTRIAN_SPEED_NOT_LOGICAL,
								String.format("[max: %.2f min: %.2f threshold: %.2f]", p.getAttributes().getMinimumSpeed(),
										p.getAttributes().getMaximumSpeed(), Pedestrian.HUMAN_MAX_SPEED))
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

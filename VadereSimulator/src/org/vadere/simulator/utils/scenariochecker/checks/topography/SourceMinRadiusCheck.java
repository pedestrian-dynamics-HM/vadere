package org.vadere.simulator.utils.scenariochecker.checks.topography;

import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.simulator.utils.scenariochecker.checks.TopographyCheckerTest;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VCircle;

import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Test if a circular source has at least 0.5 diameter
 */
public class SourceMinRadiusCheck extends AbstractScenarioCheck implements TopographyCheckerTest {
	private static final double MIN_RADIUS = 0.5;

	@Override
	public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {
		List<Source> sourceList = topography.getSources()
				.stream()
				.filter(s -> s.getShape() instanceof VCircle)
				.collect(Collectors.toList());

		for (Source source : sourceList) {
			double radius = ((VCircle) source.getShape()).getRadius();

			if (radius < MIN_RADIUS) {
				messages.add(msgBuilder.topographyWarning().target(source)
						.reason(ScenarioCheckerReason.SOURCE_TO_SMALL, ", mind. radius=" + MIN_RADIUS).build());
			}
		}

		return messages;
	}
}

package org.vadere.simulator.utils.scenariochecker.checks.topography;

import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.simulator.utils.scenariochecker.checks.TopographyCheckerTest;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;

import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class ExistingAbsorberCheck extends AbstractScenarioCheck implements TopographyCheckerTest {
    @Override
    public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {
        List<Target> targetList = topography.getTargets()
                .stream()
                .filter(Target::isAbsorbing)
                .collect(Collectors.toList());
        if ( targetList.isEmpty()) {
            messages.add(msgBuilder.topographyWarning().reason(ScenarioCheckerReason.TARGET_NO_ABSORBER,"").build());
        }
        return messages;
    }
}

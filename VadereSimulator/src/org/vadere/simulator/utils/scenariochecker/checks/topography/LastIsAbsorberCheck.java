package org.vadere.simulator.utils.scenariochecker.checks.topography;

import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.simulator.utils.scenariochecker.checks.TopographyCheckerTest;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
/**
 * Test if there are any targets with 'absorb' set to 'false' with higher index,
 * than existing targets with 'absorb' set to 'true'
 */
public class LastIsAbsorberCheck extends AbstractScenarioCheck implements TopographyCheckerTest {
    @Override
    public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {
        int maxID = topography.getTargetIds()
                .stream()
                .max(Integer::compareTo)
                .orElse(-1);
        if (maxID == -1) {
            return messages;
        }
        List<Target> targetList = topography.getTargets()
                .stream().
                filter(Target::isAbsorbing)
                .sorted(Comparator.comparingInt(Target::getId))
                .collect(Collectors.toList());
        if (targetList.isEmpty()){
            return  messages;
        }
        int lastElemID = targetList.get(targetList.size() -1 ).getId();
        if(lastElemID < maxID) {
            messages.add(msgBuilder.topographyWarning().reason(ScenarioCheckerReason.TARGET_NOT_LAST_ABSORBER,"").build());
        }
        return messages;
    }
}

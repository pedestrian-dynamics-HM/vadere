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

public class LastIsAbsorberCheck extends AbstractScenarioCheck implements TopographyCheckerTest {
    @Override
    public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {
        int maxID = topography.getTargetIds()
                .stream()
                .max(Integer::compareTo)
                .get();
        List<Target> targetList = topography.getTargets()
                .stream().
                filter(t -> t.isAbsorbing())
                .sorted((o1, o2) -> Integer.compare(o1.getId(),o2.getId()))
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

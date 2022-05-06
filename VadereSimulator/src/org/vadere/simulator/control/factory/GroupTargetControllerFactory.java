package org.vadere.simulator.control.factory;

import org.vadere.simulator.control.scenarioelements.GroupTargetController;
import org.vadere.simulator.control.scenarioelements.TargetController;
import org.vadere.simulator.models.groups.GroupIterator;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;

public class GroupTargetControllerFactory extends TargetControllerFactory {

    private final GroupIterator groupIterator;

    public GroupTargetControllerFactory(GroupIterator groupIterator) {
        this.groupIterator = groupIterator;
    }

    @Override
    public TargetController create(Topography topography, Target target) {
        return new GroupTargetController(topography, target, groupIterator);
    }
}

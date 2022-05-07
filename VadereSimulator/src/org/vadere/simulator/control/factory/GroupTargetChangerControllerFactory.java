package org.vadere.simulator.control.factory;

import org.vadere.simulator.control.scenarioelements.GroupTargetChangerController;
import org.vadere.simulator.control.scenarioelements.TargetChangerController;
import org.vadere.simulator.models.groups.GroupIterator;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.scenario.Topography;

import java.util.Random;

public class GroupTargetChangerControllerFactory extends TargetChangerControllerFactory {

    private final GroupIterator groupIterator;

    public GroupTargetChangerControllerFactory(GroupIterator groupIterator) {
        this.groupIterator = groupIterator;
    }

    @Override
    public TargetChangerController create(Topography topography, TargetChanger target, Random random) {
        return new GroupTargetChangerController(topography, target, random, groupIterator);
    }
}

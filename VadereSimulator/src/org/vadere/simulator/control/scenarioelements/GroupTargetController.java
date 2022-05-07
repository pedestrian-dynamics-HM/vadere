package org.vadere.simulator.control.scenarioelements;

import org.vadere.simulator.models.groups.Group;
import org.vadere.simulator.models.groups.GroupIterator;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;

import java.util.Iterator;

public class GroupTargetController extends TargetController {

    private final GroupIterator groupIterator;

    public GroupTargetController(Topography topography, Target target, GroupIterator groupIterator) {
        super(topography, target);
        this.groupIterator = groupIterator;
    }

    @Override
    protected boolean checkRemove(Agent agent) {
        if (!super.checkRemove(agent)) {
            assert agent instanceof Pedestrian : "GroupModel used but Agent not Pedestrian";
            Iterator<Group> iterator = groupIterator.getGroupIterator();
            while (iterator.hasNext()) {
                Group group = iterator.next();
                if (group.isMember((Pedestrian) agent)) {
                    group.checkNextTarget(target.getNextSpeed());
                    break;
                }
            }
            return false;
        }
        return true;
    }

}

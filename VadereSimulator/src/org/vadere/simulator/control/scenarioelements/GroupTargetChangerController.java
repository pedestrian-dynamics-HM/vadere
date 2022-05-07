package org.vadere.simulator.control.scenarioelements;

import org.vadere.simulator.models.groups.Group;
import org.vadere.simulator.models.groups.GroupIterator;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.scenario.Topography;

import java.util.Iterator;
import java.util.Random;

public class GroupTargetChangerController extends TargetChangerController {
    // Member Variables
    private final GroupIterator groupIterator;
    // Constructors


    public GroupTargetChangerController(Topography topography, TargetChanger targetChanger, Random random, GroupIterator groupIterator) {
        super(topography, targetChanger, random);
        this.groupIterator = groupIterator;
    }

    @Override
    protected void changeTargets(Agent agent) {
        assert agent instanceof Pedestrian : "GroupModel used but Agent not Pedestrian";
        Iterator<Group> iterator = groupIterator.getGroupIterator();
        while (iterator.hasNext()) {
            Group group = iterator.next();
            if (group.isMember((Pedestrian) agent)) {
                for (Pedestrian ped: group.getMembers()) {
                    changerAlgorithm.setAgentTargetList(ped);
                    processedAgents.put(ped.getId(), ped);
                }
                break;
            }
        }
    }
}

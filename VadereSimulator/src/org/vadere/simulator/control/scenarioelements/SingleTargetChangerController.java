package org.vadere.simulator.control.scenarioelements;

import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.scenario.Topography;

import java.util.Random;

public class SingleTargetChangerController extends TargetChangerController {

    public SingleTargetChangerController(Topography topography, TargetChanger targetChanger, Random random) {
        super(topography, targetChanger, random);
    }

    @Override
    protected void changeTargets(Agent agent) {
        changerAlgorithm.setAgentTargetList(agent);
        processedAgents.put(agent.getId(), agent);
    }
}
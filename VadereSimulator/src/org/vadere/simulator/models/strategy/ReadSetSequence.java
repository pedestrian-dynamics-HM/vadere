package org.vadere.simulator.models.strategy;

import org.apache.tools.ant.types.selectors.SelectSelector;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.LinkedList;

public class ReadSetSequence extends ReadSetControllerInputs {




    @Override
    public void update(final double simTimeInSec, final Topography top, final ProcessorManager processorManager) {


        double percentageLeft = controllerInputs[counter][1];
        getAgentsInControllerArea(top);


        if ((!newAgents.isEmpty()) && (counter % 10 == 0)) {

            printAgentTargets(simTimeInSec, percentageLeft);

            LinkedList<Integer> nextTargets = new LinkedList<Integer>();
            nextTargets.add(2001);
            nextTargets.add(1);

            if (percentageLeft == 1.0) {
                for (Pedestrian pedestrian : newAgents) {
                    pedestrian.setTargets(nextTargets);
                }
            }

            processedAgents.addAll(newAgents);
            newAgents.clear();

        }

        counter += 1;


    }


    public void printAgentTargets(double simTimeInSec, double target) {

        String info = "Time: " + String.format("%5.1fs", simTimeInSec) + ": Agents in control area: ";

        for (Pedestrian agent : newAgents) {
            info += " " + agent.getId();
        }
        if (target == 1.0) info += " sent to left corridor";
        else info += " sent to right corridor";
        logger.info(info);
    }
}

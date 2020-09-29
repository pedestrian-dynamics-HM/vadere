package org.vadere.simulator.models.strategy;

import org.vadere.simulator.control.strategy.models.navigation.INavigationModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.AttributesStrategyModel;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * The ReadSetControllerInputs is directly connected to
 * 1. the *.scenario file Scenarios/Demos/Density_controller/scenarios/TwoCorridors_forced_controller.scenario
 * 2 .the *.csv file Scenarios/Demos/Density_controller/scenarios/TwoCorridors_forced_controller_input.csv
 *
 * The ReadSetControllerInputs class serves as a simple script which allows to proceed
 * external multidimensional controller input which can not be passed as a parameter.
 *
 * In the scenario, there are two corridors.
 * The time dependent parameter "percentageLeft" controls how many agents use the left corridor.
 * If percentageLeft=0, the agents use the right and shorter path only (default OSM behavior)
 * If percentageLeft=1, the agents use the left corridor only.
 * @author  Christina Mayr
 * @since   2020-08-28
 */


public class ReadSetControllerInputs implements INavigationModel {

    private double[][] controllerInputs;
    private int counter = 0;
    private String filePath;
    private ArrayList<Pedestrian> newAgents = new ArrayList<Pedestrian>();
    private ArrayList<Pedestrian> processedAgents = new ArrayList<Pedestrian>();
    double ratioReal = 0.0;

    private static Logger logger = Logger.getLogger(ReadSetControllerInputs.class);


    @Override
    public void initialize(double simTimeInSec) {

        String fileName = this.filePath;

        try {
            this.controllerInputs = Files.lines(Paths.get(fileName)).map(s -> s.split(" ")).map(s -> Arrays.stream(s).mapToDouble(Double::parseDouble).toArray()).toArray(double[][]::new);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void update(double simTimeInSec, Topography top, ProcessorManager processorManager) {


        double percentageLeft = controllerInputs[counter][1];
        getAgentsInControllerArea(top);


        if ((!newAgents.isEmpty()) && (counter % 10 == 0)) {

            printAgentIds(simTimeInSec);

            int numberLeft = (int) (newAgents.size() * percentageLeft);
            int numberRight = newAgents.size() - numberLeft;
            ratioReal = numberLeft * 1.0 / newAgents.size();

            LinkedList<Integer> targets = new LinkedList<Integer>();
            for (int i = 0; i < numberLeft; i++) {
                targets.add(2001);
            }
            for (int i = 0; i < numberRight; i++) {
                targets.add(2002);
            }

            int c = 0;
            for (Pedestrian pedestrian : newAgents) {

                LinkedList<Integer> nextTargets = new LinkedList<Integer>();
                nextTargets.add(targets.get(c));
                nextTargets.add(1);
                pedestrian.setTargets(nextTargets);
                c += 1;
            }

            processedAgents.addAll(newAgents);
            newAgents.clear();

        }

        counter += 1;

    }

    private void printAgentIds(double simTimeInSec) {

        String info = "Time: " + String.format("%5.1fs", simTimeInSec) + ": Agents in control area: ";

        for (Pedestrian agent : newAgents) {
            info += " " + agent.getId();
        }
        logger.info(info);
    }

    @Override
    public void build(AttributesStrategyModel attr) {
        filePath = attr.getArguments().get(0); // first element contains path to file

    }

    private void getAgentsInControllerArea(Topography topography) {

        MeasurementArea m = topography.getMeasurementArea(555);
        Collection<Pedestrian> pedestrians = topography.getElements(Pedestrian.class);

        for (Pedestrian p : pedestrians) {
            if (m.getShape().contains(p.getPosition())) {
                if (!newAgents.contains(p) && !processedAgents.contains(p)) {
                    newAgents.add(p);
                }
            }
        }

    }

    @Override
    public Double getStrategyInfoForDataProcessor() {
        return ratioReal;
    }

}

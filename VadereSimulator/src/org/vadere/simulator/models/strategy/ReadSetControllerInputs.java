package org.vadere.simulator.models.strategy;

import org.vadere.simulator.control.strategy.models.navigation.INavigationModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.AttributesStrategyModel;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

// https://github.com/cschen1205/java-fuzzy-logic


public class ReadSetControllerInputs implements INavigationModel {

    private double[][] controllerInputs;
    private int counter = 0;
    private String filePath;
    private ArrayList<Pedestrian> newAgents = new ArrayList<Pedestrian>();
    private ArrayList<Pedestrian> processedAgents = new ArrayList<Pedestrian>();
    double ratioReal = -1.0;


    @Override
    public void initialize(double simTimeInSec) {

        String fileName = this.filePath;

        try {
            this.controllerInputs = Files.lines(Paths.get(fileName)).map(s -> s.split(" ")).map(s -> Arrays.stream(s).mapToDouble(Double::parseDouble).toArray()).toArray(double[][]::new);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(this.controllerInputs[0][0]);

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
            newAgents = new ArrayList<Pedestrian>();

        }

        counter += 1;

    }

    private void printAgentIds(double simTimeInSec) {

        System.out.format("Time: %.1f, \t controll agents: ", simTimeInSec);
        for (Pedestrian agent : newAgents) {
            System.out.print(" " + agent.getId());
        }
        System.out.print("\n");
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

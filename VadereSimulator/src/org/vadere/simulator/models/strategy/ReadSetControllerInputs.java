package org.vadere.simulator.models.strategy;

import com.github.cschen1205.fuzzylogic.Clause;
import com.github.cschen1205.fuzzylogic.FuzzySet;
import com.github.cschen1205.fuzzylogic.Rule;
import com.github.cschen1205.fuzzylogic.RuleInferenceEngine;
import com.github.cschen1205.fuzzylogic.memberships.FuzzyGrade;
import com.github.cschen1205.fuzzylogic.memberships.FuzzyReverseGrade;
import com.github.cschen1205.fuzzylogic.memberships.FuzzyTriangle;
import org.vadere.simulator.control.strategy.models.navigation.INavigationModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.processor.AreaDensityCountingProcessor;
import org.vadere.state.attributes.AttributesStrategyModel;
import org.vadere.state.scenario.Pedestrian;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

// https://github.com/cschen1205/java-fuzzy-logic


public class ReadSetControllerInputs implements INavigationModel {

    private double[][] controllerInputs;
    private int counter = 0;
    private String filePath;

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


    public void update(double simTimeInSec, Collection<Pedestrian> pedestrians, ProcessorManager processorManager) {


        double percentageLeft = controllerInputs[counter][1];

        List<Pedestrian> newAgents = pedestrians.stream().filter(p -> p.getFootstepHistory().getFootSteps().size() == 0).collect(Collectors.toList());

        int numberLeft = (int) (newAgents.size() * percentageLeft);
        int numberRight = newAgents.size() - numberLeft;

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

        counter += 1;

    }

    @Override
    public void build(AttributesStrategyModel attr) {
        filePath = attr.getArguments().get(0); // first element contains path to file

    }
}

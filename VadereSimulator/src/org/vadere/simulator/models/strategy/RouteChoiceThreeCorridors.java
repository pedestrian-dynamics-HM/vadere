package org.vadere.simulator.models.strategy;

import com.github.cschen1205.fuzzylogic.FuzzySet;
import com.github.cschen1205.fuzzylogic.*;
import com.github.cschen1205.fuzzylogic.memberships.*;
import org.vadere.simulator.control.strategy.models.navigation.INavigationModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.scenario.Pedestrian;

import java.util.*;
import java.util.stream.Collectors;

// https://github.com/cschen1205/java-fuzzy-logic


public class RouteChoiceThreeCorridors implements INavigationModel {

    private RuleInferenceEngine rie;
    private FuzzySet corridor;
    private FuzzySet density;
    private FuzzySet density1;
    private FuzzySet density2;
    private FuzzySet density3;


    @Override
    public void initialize(double simTimeInSec) {

        this.rie=new RuleInferenceEngine();

        double dX = 0.05;

        corridor = new FuzzySet("corridor", 2001, 2004, dX);
        corridor.addMembership("use3", new FuzzyReverseGrade(2001,2002));
        corridor.addMembership("use2", new FuzzyTriangle(2001, 2002, 2003));
        corridor.addMembership("use1", new FuzzyTriangle(2002, 2003, 2004));
        corridor.addMembership("wait", new FuzzyGrade(2003, 2004));
        rie.addFuzzySet(corridor.getName(), corridor);

        density = new FuzzySet("density", 0, 5, dX);
        density.addMembership("low", new FuzzyReverseGrade(0, 0.5));
        density.addMembership("high", new FuzzyGrade(0.4, 5));
        rie.addFuzzySet(density.getName(), density);

        density1 = new FuzzySet("density1", 0, 5, dX);
        density1.addMembership("low", new FuzzyReverseGrade(0, 0.25));
        density1.addMembership("high", new FuzzyGrade(0.2, 5));
        rie.addFuzzySet(density1.getName(), density1);

//        density2 = new FuzzySet("density2", -4, 4, 0.05);
//        density2.addMembership("low", new FuzzyReverseGrade(0, 0.25));
//        density2.addMembership("high", new FuzzyGrade(0.2, 5));
//        rie.addFuzzySet(density2.getName(), density2);
//
//        density3 = new FuzzySet("density3", -4, 4, 0.05);
//        density3.addMembership("low", new FuzzyReverseGrade(0, 0.25));
//        density3.addMembership("high", new FuzzyGrade(0.2, 5));
//        rie.addFuzzySet(density3.getName(), density3);
//


        Rule rule=new Rule("Rule 1");
        rule.addAntecedent(new Clause(density, "Is", "low"));
        rule.addAntecedent(new Clause(density1, "Is", "low"));
        rule.setConsequent(new Clause(corridor, "Is", "use1"));
        rie.addRule(rule);

        Rule rule2=new Rule("Rule 2");
        rule2.addAntecedent(new Clause(density, "Is", "high"));
        rule2.addAntecedent(new Clause(density1, "Is", "low"));
        rule2.setConsequent(new Clause(corridor, "Is", "use3"));
        rie.addRule(rule2);

        Rule rule3=new Rule("Rule 3");
        rule3.addAntecedent(new Clause(density, "Is", "high"));
        rule3.addAntecedent(new Clause(density1, "Is", "high"));
        rule3.setConsequent(new Clause(corridor, "Is", "wait"));
        rie.addRule(rule3);

        Rule rule4=new Rule("Rule 4");
        rule4.addAntecedent(new Clause(density, "Is", "low"));
        rule4.addAntecedent(new Clause(density1, "Is", "high"));
        rule4.setConsequent(new Clause(corridor, "Is", "use3"));
        rie.addRule(rule4);
        
    }



    public void update(double simTimeInSec, Collection<Pedestrian> pedestrians, ProcessorManager processorManager) {


        if (simTimeInSec > 0.0) {


            double densityC0 = getDensityFromDataProcessor(8, processorManager);
            double densityC1 = getDensityFromDataProcessor(5, processorManager);
            double densityC2 = getDensityFromDataProcessor(6, processorManager);
            double densityC3 = getDensityFromDataProcessor(7, processorManager);

            density.setX(densityC0);
            density1.setX(densityC1);
            //density2.setX(densityC2);
            //density3.setX(densityC3);
            rie.Infer(corridor);

            double targetD = corridor.getX();
            int target;

            if (Double.isNaN(targetD)) {
                target = 2002;
            }
            else{
                target = (int) Math.round(targetD);
            }

            LinkedList<Integer> nextTargets = new LinkedList<Integer>();
            nextTargets.add(target);
            if (target == 2004) {
                nextTargets.add(2001);
            }
            nextTargets.add(1);

            // System.out.println(simTimeInSec + " " + nextTargets + " \n");

            List<Pedestrian> newAgents = pedestrians.stream().filter(p -> p.getFootstepHistory().getFootSteps().size() == 0).collect(Collectors.toList());
            for (Pedestrian pedestrian : newAgents) {
                pedestrian.setTargets(nextTargets);
            }

        }

    }







    private double getDensityFromDataProcessor(int processorId, ProcessorManager processorManager) {
        double density = -1.0;
        if (processorManager != null) {
            TreeMap data = (TreeMap) processorManager.getProcessor(processorId).getData();
            if (data.size() > 0) {
                density = (double)  data.lastEntry().getValue();
            }
        }
        return density;

    }
}

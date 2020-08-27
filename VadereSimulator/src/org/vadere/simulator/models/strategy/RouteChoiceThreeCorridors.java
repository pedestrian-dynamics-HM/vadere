package org.vadere.simulator.models.strategy;

import com.github.cschen1205.fuzzylogic.FuzzySet;
import com.github.cschen1205.fuzzylogic.*;
import com.github.cschen1205.fuzzylogic.memberships.*;
import org.vadere.simulator.control.strategy.models.navigation.INavigationModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.processor.AreaDensityCountingProcessor;
import org.vadere.state.attributes.AttributesStrategyModel;
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
    private AttributesStrategyModel attributesStrategyModel;


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

        density2 = new FuzzySet("density2", -4, 4, dX);
        density2.addMembership("low", new FuzzyReverseGrade(0, 0.25));
        density2.addMembership("high", new FuzzyGrade(0.2, 5));
        rie.addFuzzySet(density2.getName(), density2);


        Rule rule;
        String ruleName;
        String[] d0 = {"high","high","high","high","low","low","low","low"};
        String[] d1 = {"high","high","low","low","high","high","low","low"};
        String[] d2 = {"high","low","high","low","high","low","high","low"};
        String[] re = {"wait","wait","wait","wait","use3","use2","use1","use1"};


        for (int i = 0; i < d0.length; i++) {

            ruleName = "Rule " + (i+1);
            rule = new Rule(ruleName);

            rule.addAntecedent(new Clause(density, "Is", d0[i]));
            rule.addAntecedent(new Clause(density1, "Is", d1[i]));
            rule.addAntecedent(new Clause(density2, "Is", d2[i]));
            rule.setConsequent(new Clause(corridor, "Is", re[i]));
            rie.addRule(rule);
        }


    }



    public void update(double simTimeInSec, Collection<Pedestrian> pedestrians, ProcessorManager processorManager) {


        if (simTimeInSec > 0.0) {


            double densityC0 = getDensityFromDataProcessor(8, processorManager);
            double densityC1 = getDensityFromDataProcessor(5, processorManager);
            double densityC2 = getDensityFromDataProcessor(6, processorManager);
            //double densityC3 = getDensityFromDataProcessor(7, processorManager);

            density.setX(densityC0);
            density1.setX(densityC1);
            density2.setX(densityC2);
            rie.Infer(corridor);

            double targetD = corridor.getX();
            int target;

            if (Double.isNaN(targetD)) {
                target = 2003;
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

    @Override
    public void build(AttributesStrategyModel attr) {
        attributesStrategyModel = attr;
    }


    private double getDensityFromDataProcessor(int processorId, ProcessorManager processorManager) {
        double density = -1.0;
        if (processorManager != null) {
            AreaDensityCountingProcessor a = (AreaDensityCountingProcessor) processorManager.getProcessor(processorId);
            TreeMap data = (TreeMap) a.getData();
            double area = a.getMeasurementArea().asPolygon().getArea();
            if (data.size() > 0) {
                density = (double) (Integer) data.lastEntry().getValue();
                density = density/area;
            }
        }
        return density;

    }
}

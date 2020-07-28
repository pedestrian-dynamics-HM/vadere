package org.vadere.simulator.models.strategy;

import net.sourceforge.jFuzzyLogic.plot.JFuzzyChart;
import net.sourceforge.jFuzzyLogic.rule.Rule;
import org.vadere.simulator.control.strategy.models.navigation.INavigationModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.scenario.Pedestrian;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import net.sourceforge.jFuzzyLogic.FIS; // http://jfuzzylogic.sourceforge.net/html/index.html

public class RouteChoiceThreeCorridors implements INavigationModel {



    public void update(double simTimeInSec, Collection<Pedestrian> pedestrians, ProcessorManager processorManager) {

        String fileName = "/home/christina/repos/vadere/VadereSimulator/src/org/vadere/simulator/models/strategy/fcl/tipper.fcl";

        FIS fis = FIS.load(fileName, true); // Load from 'FCL' file
        fis.setVariable("density", 30); // Set inputs
        fis.setVariable("densityCor1", 70);
        fis.setVariable("densityCor2", 70);
        fis.setVariable("densityCor3", 70);
        fis.evaluate(); // Evaluate
        System.out.println("Output value:" + fis.getVariable("corridor").getValue()); // Show output variable

        // Show each rule (and degree of support)
        for( Rule r : fis.getFunctionBlock("tipper").getFuzzyRuleBlock("No1").getRules() ) System.out.println(r);


        if (simTimeInSec > 0.0) {
            // get data from dataprocessors if necessary
            LinkedList<Double> densities = new LinkedList<Double>();
            densities.add(getDensityFromDataProcessor(5, processorManager));
            densities.add(getDensityFromDataProcessor(6, processorManager));
            densities.add(getDensityFromDataProcessor(7, processorManager));

            double density = getDensityFromDataProcessor(8, processorManager);

            double maxDensity = 10.0;
            double remainingCapacity;
            LinkedList<Double> factors = new LinkedList<Double>();
            LinkedList<Integer> factorsNorm = new LinkedList<Integer>();
            double sum = 0;

            if (density > maxDensity) {
                for (Double d : densities) {
                    remainingCapacity = maxDensity - d;

                    double fac = 0;
                    if (remainingCapacity > 0) {
                        fac = 1;
                    }
                    factors.add(fac);
                    sum += fac;
                }

            } else {
                factors.add(0.0);
                factors.add(0.0);
                factors.add(1.0);
                sum = 1.0;
            }


            List<Pedestrian> newAgents = pedestrians.stream().filter(p -> p.getFootstepHistory().getFootSteps().size() == 0).collect(Collectors.toList());
            int numberOfNewAgents = (int) newAgents.size();

            System.out.println(simTimeInSec);

/*            if (numberOfNewAgents > 0) {
                if (numberOfNewAgents == 1){ sum = 1.0;}

                int[] target = {2001, 2002, 2003};
                int c;

                boolean check = false;

                while (!check) {
                    c = 0;
                    for (Double f : factors) {
                        int n = (int) (f / sum * numberOfNewAgents);
                        for (int i = 0; i < n; i++) {
                            System.out.println("Add " + target[c] );
                            factorsNorm.add(target[c]);
                            if (factorsNorm.size() >= numberOfNewAgents) {
                                check = true;
                                break;
                            }
                        }
                        c += 1;
                    }
                }


                System.out.println(" ---- > Number " + numberOfNewAgents + ", targets " + factorsNorm);

                LinkedList<Integer> nextTargets = new LinkedList<Integer>();

                c = 0;
                for (Pedestrian pedestrian : newAgents) {
                    nextTargets.add(factorsNorm.get(c));
                    pedestrian.setTargets(nextTargets);
                    c += 1;
                }
            }*/

        }

    }



//    @Override
//   public void update(double simTimeInSec, Collection<Pedestrian> pedestrians, ProcessorManager processorManager) {

//        if (simTimeInSec > 0.0) {
//            // get data from dataprocessors if necessary
//            double density1 = getDensityFromDataProcessor(5, processorManager);
//            double density2 = getDensityFromDataProcessor(6, processorManager);
//            double density3 = getDensityFromDataProcessor(7, processorManager);
//            double density = getDensityFromDataProcessor(8, processorManager);
//
//            LinkedList<Integer> nextTargets = new LinkedList<Integer>();
//            int newTarget = 2003;
//
//            if (density > 10.0){
//                newTarget = 2002;}
//
//            if (density > 20.0){
//                newTarget = 2001;}
//
//            for (Pedestrian pedestrian : pedestrians) {
//                if (pedestrian.getFootstepHistory().size() == 0) {
//                    nextTargets.add(newTarget);
//                    pedestrian.setTargets(nextTargets);
//                }
//            }
//
//        }
//    }


    private double getDensityFromDataProcessor(int processorId, ProcessorManager processorManager) {
        double density = -1.0;
        if (processorManager != null) {
            TreeMap data = (TreeMap) processorManager.getProcessor(processorId).getData();
            if (data.size() > 0) {
                density = (double) (int) data.lastEntry().getValue();
            }
        }
        return density;

    }
}

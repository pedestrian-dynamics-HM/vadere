package org.vadere.simulator.models.strategy;

import net.sourceforge.jFuzzyLogic.FIS;
import org.vadere.simulator.control.strategy.models.navigation.INavigationModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.scenario.Pedestrian;

import java.util.*;
import java.util.stream.Collectors;


public class RouteChoiceThreeCorridors implements INavigationModel {

    private FIS fis;

    @Override
    public void initialize(double simTimeInSec) {
        // load FuzzyControllLanguage file which stores the logic of the controller (heuristics)

        String fileName = "/home/christina/repos/vadere/VadereSimulator/src/org/vadere/simulator/models/strategy/fcl/tipper.fcl";
        this.fis = FIS.load(fileName, true); // Load from 'FCL' file
    }



    public void update(double simTimeInSec, Collection<Pedestrian> pedestrians, ProcessorManager processorManager) {


        if (simTimeInSec > 0.0) {

            System.out.println(simTimeInSec);

            double densityCor1 = getDensityFromDataProcessor(5, processorManager);
            double densityCor2 = getDensityFromDataProcessor(6, processorManager);
            double densityCor3 = getDensityFromDataProcessor(7, processorManager);
            double density = getDensityFromDataProcessor(8, processorManager);

            LinkedList<Integer> nextTargets = new LinkedList<Integer>();
            int target = getTargetFromFuzzyController(density,densityCor1,densityCor2,densityCor3);

            List<Pedestrian> newAgents = pedestrians.stream().filter(p -> p.getFootstepHistory().getFootSteps().size() == 0).collect(Collectors.toList());
            for (Pedestrian pedestrian : newAgents) {
                nextTargets.add(target);
                pedestrian.setTargets(nextTargets);
            }

        }

    }

    private int getTargetFromFuzzyController(double density,double densityCor1,double densityCor2,double densityCor3) {

        fis.setVariable("density", density); // Set inputs
        fis.setVariable("densityCor1", densityCor3);
        fis.setVariable("densityCor2", densityCor2);
        fis.setVariable("densityCor3", densityCor1);
        fis.evaluate(); // Evaluate

        double corridor = fis.getVariable("corridor").getValue();
        int result = (int) Math.round(corridor);

        System.out.println("Densities: " + density + " " + densityCor1 + " " + densityCor2 +" " + densityCor3); // Show output variable

        System.out.println("Output value:" + corridor + ", rounded :" + result); // Show output variable

        // Show each rule (and degree of support)
        //for( Rule r : fis.getFunctionBlock("streamControl").getFuzzyRuleBlock("No1").getRules() ) System.out.println(r);

        return result;
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

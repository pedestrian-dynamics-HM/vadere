package org.vadere.simulator.models.strategy;

import org.apache.commons.math.distribution.DiscreteDistribution;
import org.vadere.simulator.control.strategy.models.navigation.INavigationModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.scenario.Pedestrian;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.tools.ant.taskdefs.rmic.DefaultRmicAdapter.rand;

public class RouteChoiceThreeCorridors implements INavigationModel {


    @Override
    public void update(double simTimeInSec, Collection<Pedestrian> pedestrians, ProcessorManager processorManager) {

        if (simTimeInSec > 0.0) {
            // get data from dataprocessors if necessary
            LinkedList<Double> densities = new LinkedList<Double>();
            densities.add( getDensityFromDataProcessor(5, processorManager) );
            densities.add( getDensityFromDataProcessor(6, processorManager) );
            densities.add( getDensityFromDataProcessor(7, processorManager) );

            double density = getDensityFromDataProcessor(8, processorManager);

            double maxDensity = 10.0;
            double remainingCapacity;
            LinkedList<Double> factors = new LinkedList<Double>();
            LinkedList<Integer> factorsNorm = new LinkedList<Integer>();
            double sum = 0;

            if (density > maxDensity){
                for (Double d : densities) {
                    remainingCapacity = maxDensity - d;

                    double fac = 0;
                    if (remainingCapacity > 0) {
                        fac = 1;
                    }
                    factors.add(fac);
                    sum += fac;
                }

            }
            else{
                factors.add(0.0);
                factors.add(0.0);
                factors.add(1.0);
                sum = 1.0;
            }



            List<Pedestrian> newAgents  =  pedestrians.stream().filter(p-> p.getFootstepHistory().getFootSteps().size() == 0).collect(Collectors.toList());
            int numberOfNewAgents = (int) newAgents.size();

            int[] target = {2001,2002,2003};
            int c = 0;
            for (Double f : factors){
                int n = (int) (f/sum*numberOfNewAgents);
                for (int i = 0; i < n; i++){
                    factorsNorm.add( target[c] );
                }
                c += 1;
            }

            LinkedList<Integer> nextTargets = new LinkedList<Integer>();

            c = 0;
            for (Pedestrian pedestrian : newAgents) {
                nextTargets.add(factorsNorm.get(c));
                pedestrian.setTargets(nextTargets);
                c+=1;
            }

        }

    }



  /*  public void update(double simTimeInSec, Collection<Pedestrian> pedestrians, ProcessorManager processorManager) {

        if (simTimeInSec > 0.0) {
            // get data from dataprocessors if necessary
            double density1 = getDensityFromDataProcessor(5, processorManager);
            double density2 = getDensityFromDataProcessor(6, processorManager);
            double density3 = getDensityFromDataProcessor(7, processorManager);
            double density = getDensityFromDataProcessor(8, processorManager);

            LinkedList<Integer> nextTargets = new LinkedList<Integer>();
            int newTarget = 2003;

            if (density > 10.0){
                newTarget = 2002;}

            if (density > 20.0){
                newTarget = 2001;}

            for (Pedestrian pedestrian : pedestrians) {
                if (pedestrian.getFootstepHistory().size() == 0) {
                    nextTargets.add(newTarget);
                    pedestrian.setTargets(nextTargets);
                }
            }

        }

    }*/


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

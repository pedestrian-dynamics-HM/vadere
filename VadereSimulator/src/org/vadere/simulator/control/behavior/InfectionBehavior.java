/**
 * Author: Mina Abadeer
 * Group Parallel and Distributed Systems
 * Department of Computer Science
 * University of Muenster, Germany
 *
 * Co-author(s):
 * Sameh Magharious
 * Dell Technologies, USA
 *
 * Hesham Hussen
 * HAW Hamburg, Germany
 *
 * This software is licensed under the GNU Lesser General Public License (LGPL).
 */
package org.vadere.simulator.control.behavior;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.osm.stairOptimization.StairStepOptimizer;
import org.vadere.simulator.projects.Scenario;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.infection.AttributesInfectionBehavior;
import org.vadere.state.infection.InfectionHistory;
import org.vadere.state.infection.PedestrianInfectionType;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.text.CollationElementIterator;
import java.util.*;
import java.util.stream.Collectors;

public class InfectionBehavior extends PedestrianBehavior<Pedestrian> {
    private static Logger log = Logger.getLogger(InfectionBehavior.class);

    @Getter
    public double infectionRate = 0;
    private int totalSteps = 1;
    private AttributesInfectionBehavior attributes;
    private int initiallyInfectedCount;
    private int limit;
    private int initiallyInfectedCounter = 0;
    //TODO: Consider a state transition manager (between various infection statuses) to simplify the logic here
    @Override
    public void apply(final Collection<Pedestrian> pedestrians, final Topography scenario) {
        initialInfection(pedestrians);
        isolateInfected(pedestrians);
        processPedestriansInInfectionZone(pedestrians, scenario);
        updateInfectionHistoryForAllPedestrians(pedestrians);
        totalSteps++;
    }
    // TODO: different isolation methodology
    private void isolateInfected(Collection<Pedestrian> pedestrians) {

    }


    private void initialInfection(Collection<Pedestrian> pedestrians) {
        if (initiallyInfectedCounter < initiallyInfectedCount ) {
            log.info( "Step number: " + totalSteps);
            pedestrians.stream().filter(ped -> ped.isSusceptible())
                    .limit(limit)
                    .forEach(susceptible -> susceptible.setInfectionStatus(PedestrianInfectionType.INFECTED));
            initiallyInfectedCounter += limit;
        }
    }

    private void processPedestriansInInfectionZone(final Collection<Pedestrian> pedestrians, final Topography scenario) {
        int toBeInfectedCount = 0;
        var infectedPeds = pedestrians.stream().filter(p -> p.isInfected()).collect(Collectors.toList());
        var infectionZonePeds = new ArrayList<Pedestrian>();
        for (var infectedPed : infectedPeds) {
            double infectionRadius = getInfectionRadius(infectedPed);
            infectionZonePeds.addAll(scenario.getSpatialMap(Pedestrian.class)
                    .getObjects(infectedPed.getPosition(), infectionRadius));
        }

        for (var ped : infectionZonePeds) {
            if(ped.getInfectionStatus() != PedestrianInfectionType.SUSCEPTIBLE) continue;
            InfectionHistory infectionHistory = ped.getInfectionHistory();
            if (infectionHistory.isInInfectionZoneLastStep()) {
                infectionHistory.incrementTotalStepsInInfectionZone();
                if(infectionHistory.getTotalStepsInInfectionZone() >= attributes.getDurationToTransmitInSteps()){
                    toBeInfectedCount++;
                    ped.setInfectionStatus(PedestrianInfectionType.INFECTED);
                    infectionHistory.resetTotalStepsInInfectionZone();
                }
            } else {
                infectionHistory.intoInfectionZone();
            }

            //The number of previously infected pedestrians (since start until last step)
            //infectedPeds.size()

            //The number of newly infected pedestrians in current step
            //toBeInfectedCount

            //Total number of peds
            //pedestrians.size()

            //infectionRate per step (%)
            infectionRate = (double) toBeInfectedCount/ totalSteps*100;
            scenario.setInfectionRate(infectionRate);
        }
    }

    private void updateInfectionHistoryForAllPedestrians(final Collection<Pedestrian> pedestrians) {
        for (var ped : pedestrians){
            InfectionHistory infectionHistory = ped.getInfectionHistory();
            switch (ped.getInfectionStatus()) {
                case INFECTED: {
                    infectionHistory.incrementTotalStepsSinceInfected();
                    if(infectionHistory.getTotalStepsSinceInfected() >= attributes.getRecoveryDurationInSteps()){
                        ped.setInfectionStatus(PedestrianInfectionType.RECOVERED);
                        infectionHistory.resetTotalStepsSinceInfected();
                    }
                }
                break;
                case RECOVERED: {
                    infectionHistory.incrementTotalStepsSinceRecovered();
                    if(infectionHistory.getTotalStepsSinceRecovered() >= attributes.getImmunityDurationInSteps()){
                        infectionHistory.resetTotalStepsSinceRecovered();
                        ped.setInfectionStatus(PedestrianInfectionType.SUSCEPTIBLE);
                    }
                }
                break;
            }
        }
    }

    /**
     * This method calculate the infectious radius every update. The value of the infectious radius calculated
     * based on the selected protective measures.
     * In order to test the behavior, we set it here to be a fixed value (PedRadius*5)
     */
    private double getInfectionRadius(final Pedestrian infectedPed) {
        return infectedPed.getRadius() * 5;
    }

    /**
     * This is the naive infection behavior method that doesn't take into account any of the protective measures.
     * Kept here for reference
     *
     * @param pedestrians the list of pedestrians in the scenario
     */
    private void applyNaiveInfectionBehavior(final Collection<Pedestrian> pedestrians) {
        var toBeInfected = new ArrayList<Pedestrian>();
        var infectedPeds = pedestrians.stream().filter(p -> p.isInfected()).collect(Collectors.toList());

        for (var infectedPed : infectedPeds) {
            for (var ped : pedestrians) {
                if (ped == infectedPed) continue;

                var distance = infectedPed.getPosition().distance(ped.getPosition());

                //if the distance between the centers of the two pedestrians is less than twice their radius
                //then they are in contact, and the infection should be transmitted
                if (distance <= (infectedPed.getRadius() * 2) + attributes.getMinInfectionDistance()) {
                    toBeInfected.add(ped);
                }
            }
        }

        toBeInfected.stream().forEach(p -> p.setInfectionStatus(PedestrianInfectionType.INFECTED));
    }

    @Override
    public void initialize(final List<Attributes> modelAttributesList, final Topography scenario) {
        this.attributes = Model.findAttributes(modelAttributesList, AttributesInfectionBehavior.class);


        var totalSpawnedPerStep = scenario.getSources().stream().mapToInt(s -> s.getAttributes().getSpawnNumber()).sum();

        var totalSpawned = scenario.getSources().stream().mapToInt(s -> s.getAttributes().getMaxSpawnNumberTotal()).sum();
        if (totalSpawned < 0) {
            totalSpawned = totalSpawnedPerStep;
        }
        this.initiallyInfectedCount = (int) (attributes.getInfectionPercentage() * totalSpawned);
        log.info("Total: " + totalSpawned + ", total spawned per Step: " + totalSpawnedPerStep + ", initially infected: " + initiallyInfectedCount );

        // average Spawn Steps
        var averageSteps = totalSpawned / totalSpawnedPerStep ;
        this.limit = initiallyInfectedCount < averageSteps ? initiallyInfectedCount : initiallyInfectedCount / averageSteps ;
        log.info("Spawn steps: " + averageSteps + ", limit: " + limit);

    }
}

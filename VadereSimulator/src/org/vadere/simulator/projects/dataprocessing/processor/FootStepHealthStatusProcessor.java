package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;

import java.util.LinkedList;
import java.util.Locale;

/**
 * Log {@link Pedestrian}'s current {@link org.vadere.state.health.ExposureModelHealthStatus}
 */

@DataProcessorClass()
public class FootStepHealthStatusProcessor extends DataProcessor<EventtimePedestrianIdKey, String> {
    public static String[] HEADERS = {"isInfectious", "degreeOfExposure"};

    public FootStepHealthStatusProcessor() {
        super(HEADERS);
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        for (Pedestrian pedestrian : state.getTopography().getElements(Pedestrian.class)) {
            LinkedList<FootStep> footSteps = pedestrian.getTrajectory().clone().getFootSteps();


            String healthStatusAsString = healthStatusToString(pedestrian);

            for (FootStep footStep : footSteps) {
                putValue(new EventtimePedestrianIdKey(footStep.getStartTime(), pedestrian.getId()), healthStatusAsString);
            }
        }
    }

    private String healthStatusToString(Pedestrian pedestrian) {
        String statusAsString = String.format(Locale.US, "%s %f",
                pedestrian.isInfectious(),
                pedestrian.getDegreeOfExposure()
        );

        return statusAsString;
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
    }

}

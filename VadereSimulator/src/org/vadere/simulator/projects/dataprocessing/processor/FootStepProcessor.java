package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.VTrajectory;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * <p>During one time step a pedestrian my move multiple times which is saved by {@link Pedestrian#getTrajectory()}, i.e.
 * the {@link VTrajectory} will be adjusted after each update(simTimeInSec) call such that it contains the foot steps
 * which started at the lastSimTimeInSec!</p>
 *
 * <p>This processor writes out all those {@link FootStep}s. The index is the simulation time where the event occurred
 * and the pedestrian id. The "endTime" of the foot step is part of the data. Each row corresponds to one foot step.
 * </p>
 *
 * <p>This is especially useful if one uses the {@link org.vadere.simulator.models.osm.OptimalStepsModel} or any other
 * {@link org.vadere.simulator.models.MainModel} for which pedestrians do multiple steps during a simulation time step.
 * </p>
 *
 * @author Benedikt Zoennchen
 */
@DataProcessorClass()
public class FootStepProcessor extends DataProcessor<EventtimePedestrianIdKey, FootStep> {

    public FootStepProcessor() {
        super("endTime", "startX", "startY", "endX", "endY");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        for (Pedestrian pedestrian : state.getTopography().getElements(Pedestrian.class)) {
            LinkedList<FootStep> footSteps = pedestrian.getTrajectory().clone().getFootSteps();

            for (FootStep fs : footSteps) {
                putValue(new EventtimePedestrianIdKey(fs.getStartTime(), pedestrian.getId()), fs);
            }

            //TODO: refactor
            if ((footSteps.size() == 0) && (state.getStep() == 1)
                    && ((pedestrian.getSelfCategory() == SelfCategory.WAIT) || (pedestrian.getSelfCategory() == SelfCategory.CHANGE_TARGET))) {
                // add dummy footstep for self category visualization
                FootStep fs = new FootStep(pedestrian.getPosition(), pedestrian.getPosition(), 0.0, -1); // -1 needs to be corrected in the postloop
                putValue(new EventtimePedestrianIdKey(state.getSimTimeInSec(), pedestrian.getId()), fs);
            }


        }
    }

    public void postLoop(final SimulationState state) {

        // add correct end time to dummy footstep
        //TODO: refactor
        for (EventtimePedestrianIdKey key : this.getData().keySet()){
            FootStep firstFootstep = this.getValue(key);
            if (firstFootstep.getEndTime() == -1) {
                for (EventtimePedestrianIdKey nextFootstep : this.getData().keySet()) {
                    if (nextFootstep.getPedestrianId().equals(key.getPedestrianId()) &&
                            this.getValue(nextFootstep).getStartTime() > 0.0) {
                        FootStep fs = new FootStep(firstFootstep.getStart(),
                                firstFootstep.getEnd(),
                                firstFootstep.getStartTime(),
                                this.getValue(nextFootstep).getStartTime());
                        this.getData().replace(key, fs);
                        break;
                    }
                }
            }
        }
    }



    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
    }

    @Override
    public String[] toStrings(EventtimePedestrianIdKey key) {

        String[] footStepLine = this.getValue(key).getValueString();

        // Note: remove the "startTime" from the footStepLine because it is already included in the "simTime" of the
        // EventtimePedestrianIdKey
        return Arrays.copyOfRange(footStepLine, 1, footStepLine.length);
    }
}
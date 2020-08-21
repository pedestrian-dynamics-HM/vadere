package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.state.attributes.processor.AttributesEvacuationTimeProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.Collections;

/**
 * @author Mario Teixeira Parente
 * @author Marion Gödel
 * EvacuationTimeProcessor measures the time difference between the first agent entering the simulation
 * (using PedestrianStartTimeProcessor) and the last agent leaving the simulation (PedestrianEndTimeProcessor).
 * In previous versions, the PedestrianEvacuationTimeProcessor has been used for this instead.
 * todo check if all sources have finished spawning.
 *
 */
@DataProcessorClass()
public class EvacuationTimeProcessor extends NoDataKeyProcessor<Double> {
    private PedestrianStartTimeProcessor pedestrianStartTimeProcessor;
    private PedestrianEndTimeProcessor pedestrianEndTimeProcessor;
    private int numberOfAgentsInScenario = 0;

    public EvacuationTimeProcessor() {
        super("evacuationTime");
        setAttributes(new AttributesEvacuationTimeProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        this.pedestrianStartTimeProcessor.update(state);
        this.pedestrianEndTimeProcessor.update(state);
        this.numberOfAgentsInScenario = state.getTopography().getPedestrianDynamicElements().getElements().size();
    }

    @Override
    public void postLoop(final SimulationState state) {
        this.pedestrianStartTimeProcessor.postLoop(state);
        this.pedestrianEndTimeProcessor.postLoop(state);

        double result = Double.NaN;

        // check if any agents are still in the simulation
        // seems like the topography is empty after the simulation, so I saved the last value in numberOfAgentsInScenario
        // todo only the SourceController knows if the source has finished spawning, but we have no access (part of Simulation)
        if(numberOfAgentsInScenario == 0){
            if (this.pedestrianEndTimeProcessor.getValues().size() > 0) {
                double maxPedEndTime = this.pedestrianEndTimeProcessor.getValues().stream().anyMatch(tevac -> tevac == Double.NaN)
                        ? Double.NaN
                        : Collections.max(this.pedestrianEndTimeProcessor.getValues());

                double minPedStartTime = this.pedestrianEndTimeProcessor.getValues().stream().anyMatch(tevac -> tevac == Double.NaN)
                        ? Double.NaN
                        : Collections.min(this.pedestrianEndTimeProcessor.getValues());
                result = maxPedEndTime - minPedStartTime;
            }
        }else{
            result = -1;
        }

        this.putValue(NoDataKey.key(), result);
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesEvacuationTimeProcessor att = (AttributesEvacuationTimeProcessor) this.getAttributes();
        this.pedestrianStartTimeProcessor = (PedestrianStartTimeProcessor) manager.getProcessor(att.getPedestrianStartTimeProcessorId());
        this.pedestrianEndTimeProcessor = (PedestrianEndTimeProcessor) manager.getProcessor(att.getPedestrianEndTimeProcessorId());

    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesEvacuationTimeProcessor());
        }

        return super.getAttributes();
    }
}

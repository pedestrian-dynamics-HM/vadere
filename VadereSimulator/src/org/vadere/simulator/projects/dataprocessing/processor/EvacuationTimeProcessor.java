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
 *
 */
@DataProcessorClass()
public class EvacuationTimeProcessor extends NoDataKeyProcessor<Double> {
    private PedestrianEvacuationTimeProcessor pedEvacTimeProc;
    private int numberOfAgentsInScenario = 0;

    public EvacuationTimeProcessor() {
        super("evacuationTime");
        setAttributes(new AttributesEvacuationTimeProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        this.pedEvacTimeProc.update(state);
        this.numberOfAgentsInScenario = state.getTopography().getPedestrianDynamicElements().getElements().size();
    }

    @Override
    public void postLoop(final SimulationState state) {
        this.pedEvacTimeProc.postLoop(state);

        double result = 0.0;

        // check if any agents are still in the simulation
        // seems like the topography is empty after the simulation, so I saved the last value in numberOfAgentsInScenario
        if(numberOfAgentsInScenario == 0){
            if (this.pedEvacTimeProc.getValues().size() > 0) {
                result = this.pedEvacTimeProc.getValues().stream().anyMatch(tevac -> tevac == Double.NaN)
                        ? Double.NaN
                        : Collections.max(this.pedEvacTimeProc.getValues());
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
        this.pedEvacTimeProc = (PedestrianEvacuationTimeProcessor) manager.getProcessor(att.getPedestrianEvacuationTimeProcessorId());
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesEvacuationTimeProcessor());
        }

        return super.getAttributes();
    }
}

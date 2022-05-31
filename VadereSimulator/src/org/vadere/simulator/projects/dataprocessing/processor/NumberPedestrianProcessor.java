package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.scenario.Pedestrian;

@DataProcessorClass(label = "NumberPedestrianProcessor")
public class NumberPedestrianProcessor extends DataProcessor<TimestepKey, Integer> {


    public NumberPedestrianProcessor(){
        super("NumAgents");
    }

    @Override
    protected void doUpdate(SimulationState state) {
        int numAgents = state.getTopography().getElements(Pedestrian.class).size();
        putValue(new TimestepKey(state.getStep()), numAgents);
    }


}

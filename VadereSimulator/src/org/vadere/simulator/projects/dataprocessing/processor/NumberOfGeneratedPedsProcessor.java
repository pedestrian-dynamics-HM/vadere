package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.collections.CollectionUtils;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.processor.AttributesNumberOfGeneratedPedsProcessor;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@DataProcessorClass(label = "NumberOfGeneratedPedsProcessor")
public class NumberOfGeneratedPedsProcessor extends DataProcessor<TimestepKey, Double> {

    private List<Integer> pedIds = new ArrayList<Integer>();

    public NumberOfGeneratedPedsProcessor(){
        super("NumAgentsGenPerSecond");
    }

    @Override
    protected void doUpdate(SimulationState state) {

        double t = state.getSimTimeInSec();

        if ( t + 1e-7 >= getAttributes().getStartTime() &&  t - 1e-7 <= getAttributes().getEndTime() ) {

            int numAgentsGen;
            List<Integer> newPedIds = state.getTopography().getElements(Pedestrian.class).stream().map(Agent::getId).collect(Collectors.toList());

            if (this.getPedsIds() == null){
                numAgentsGen = newPedIds.size();
            }
            else {
                List<Integer> oldPedIds = this.getPedsIds();
                List<Integer> list = new ArrayList<Integer>(CollectionUtils.disjunction(newPedIds, oldPedIds));
                list.removeAll(oldPedIds);
                numAgentsGen = list.size();
            }
            this.setPedsIds(newPedIds);

            double poissonParameter = numAgentsGen/state.getScenarioStore().getAttributesSimulation().getSimTimeStepLength();
            putValue(new TimestepKey(state.getStep()), poissonParameter);
        }

    }

    @Override
    public AttributesNumberOfGeneratedPedsProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesNumberOfGeneratedPedsProcessor());
        }
        return (AttributesNumberOfGeneratedPedsProcessor)super.getAttributes();
    }


    private List<Integer> getPedsIds(){
        return this.pedIds;
    }

    private void setPedsIds(List<Integer> pedsId){
        this.pedIds = pedsId;
    }




}

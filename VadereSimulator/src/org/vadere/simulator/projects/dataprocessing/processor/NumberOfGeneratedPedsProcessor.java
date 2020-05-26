package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.collections.CollectionUtils;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
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

    private List<Integer> pedIds;

    public NumberOfGeneratedPedsProcessor(){
        super("numberPedsGenerated");
    }

    @Override
    public void init(ProcessorManager manager) {
        super.init(manager);
        // setup filter

    }


    @Override
    protected void doUpdate(SimulationState state) {

        Collection<Pedestrian> peds2 = state.getTopography().getElements(Pedestrian.class);
        List<Integer> newPedIds = peds2.stream().map(Agent::getId).collect(Collectors.toList());

        if (this.getPedsIds() == null){
            this.setPedsIds(newPedIds);
        }

        List<Integer> oldPedIds = this.getPedsIds();
        List<Integer> list = new ArrayList<Integer>(CollectionUtils.disjunction(newPedIds, oldPedIds));
        list.removeAll(oldPedIds);

        this.setPedsIds(newPedIds);

        double poissonParameter = list.size()/state.getScenarioStore().getAttributesSimulation().getSimTimeStepLength();

        putValue(new TimestepKey(state.getStep()), poissonParameter);
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

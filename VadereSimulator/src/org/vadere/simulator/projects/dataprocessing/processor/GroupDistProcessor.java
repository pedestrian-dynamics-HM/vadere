package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.procesordata.AreaGroupMetaData;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepGroupIdKey;
import org.vadere.state.attributes.processor.AttributesGroupDistProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Manuel Hertle
 */


public abstract class GroupDistProcessor extends DataProcessor<TimestepGroupIdKey, Double> {
    private AreaGroupMetaDataProcessor groupMetaDataProc;

    @Override
    public void doUpdate(final SimulationState state) {
        this.groupMetaDataProc.update(state);
        double simTime = state.getSimTimeInSec();

        //get group data for timestep
        Map<TimestepGroupIdKey, AreaGroupMetaData> groupMetaData = this.groupMetaDataProc.getData();
        groupMetaData = groupMetaData.entrySet().stream()
                .filter(e -> e.getKey().getTimestep() == state.getStep())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (AreaGroupMetaData singleMetaData : groupMetaData.values()) {
            int groupId = singleMetaData.getGroup().getID();
            Optional<Double> distance = getDistance(singleMetaData, groupMetaData.values());
            if (distance.isPresent()) {
                this.putValue(new TimestepGroupIdKey(state.getStep(), groupId), distance.get());
            } else {
                this.putValue(new TimestepGroupIdKey(state.getStep(), groupId), Double.NaN);
            }
        }
    }

    public Optional<Double> getDistance(AreaGroupMetaData from, Collection<AreaGroupMetaData> to) {
        return Optional.empty();
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesGroupDistProcessor attGrMinDist = (AttributesGroupDistProcessor) this.getAttributes();

        this.groupMetaDataProc =
                (AreaGroupMetaDataProcessor) manager.getProcessor(attGrMinDist.getAreaGroupMetaDataProcessorId());
    }

    @Override
    public AttributesProcessor getAttributes() {
        if (super.getAttributes() == null) {
            setAttributes(new AttributesGroupDistProcessor());
        }

        return super.getAttributes();
    }
}

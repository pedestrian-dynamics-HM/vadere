package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.procesordata.AreaGroupMetaData;
import org.vadere.simulator.models.groups.cgm.CentroidGroup;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepGroupIdKey;
import org.vadere.state.attributes.processor.AttributesGroupMetaDataProcessor;
import org.vadere.state.attributes.processor.AttributesAreaProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.simulator.projects.dataprocessing.processor.util.ModelFilter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Manuel Hertle
 *
 * Metadata considering group members in measuring area
 */

@DataProcessorClass(label = "AreaGroupMetaDataProcessor")
public class AreaGroupMetaDataProcessor extends DataProcessor<TimestepGroupIdKey, AreaGroupMetaData> implements ModelFilter {

    private MeasurementArea measurementArea;

    public AreaGroupMetaDataProcessor() {
        super("sim_time", "ped_total", "memb_in_area", "peds_lost", "centroid_x", "centroid_y");
        setAttributes(new AttributesGroupMetaDataProcessor());
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesAreaProcessor att = (AttributesAreaProcessor) this.getAttributes();
        if (att.getMeasurementAreaId() != -1) {
            this.measurementArea = manager.getMeasurementArea(att.getMeasurementAreaId(), false);
        }
    }

    @Override
    protected void doUpdate(final SimulationState state) {

        double simTime = state.getSimTimeInSec();

        //collect all pedestrians from state
        Collection<Pedestrian> pedestrians = state.getTopography().getPedestrianDynamicElements().getElements();

        // the pedestrians in the measurement area
        if (measurementArea != null) {
            pedestrians = pedestrians.stream()
                    .filter(p -> this.measurementArea.getShape().contains(p.getPosition()))
                    .collect(Collectors.toList());
        }

        // compute the id of groups represented in the measurement area
        Set<Integer> groupIdsInArea = new HashSet<>();
        for (Pedestrian p : pedestrians) {
            LinkedList<Integer> groupIds = p.getGroupIds();
            groupIdsInArea.addAll(groupIds);
        }

        try {
            // find CentroidGroupModel
            //TODO downcasting is breaking SOLID Principles
            CentroidGroupModel model = (CentroidGroupModel) getModel(state, CentroidGroupModel.class).get();

            //find groups represented in Area
            Map<Integer, CentroidGroup> allGroups = model.getGroupsById();
            List<CentroidGroup> groupsInArea = allGroups.entrySet().stream()
                    .filter(all -> groupIdsInArea.contains(all.getKey()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            for (CentroidGroup currentGroup: groupsInArea) {
                List<Pedestrian> membersInArea = currentGroup.getMembers().stream()
                        .filter(pedestrians::contains)
                        .collect(Collectors.toList());
                long pedsLostInArea = membersInArea.stream()
                        .filter(ped -> currentGroup.getLostMembers().contains(ped))
                        .count();
                AreaGroupMetaData data = new AreaGroupMetaData(currentGroup, membersInArea.size(), membersInArea,
                        pedestrians.size(), pedsLostInArea);
                data.setSimTime(simTime);
                try {
                    //compute the convex hull for all groups and the centroid of the resulting polygons -> problem might be that
                    //convex hull is susceptible to statistical outliers
                    VPoint centroid = currentGroup.getCentroid(false);
                    data.setCentroid(centroid);
                    if (this.measurementArea != null) {
                        data.setCentroidInArea(this.measurementArea.getShape().contains(centroid));
                    }
                } catch (IllegalArgumentException e) {
                }
                this.putValue(new TimestepGroupIdKey(state.getStep(), currentGroup.getID()), data);
            }
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String[] toStrings(TimestepGroupIdKey key) {
        AreaGroupMetaData groupInArea = this.getValue(key);
        if(groupInArea == null) {
            return new String[]{"NA", "NA", "NA", "NA", "NA", "NA"};
        }
        else {
            if (groupInArea.getCentroid().isEmpty()) {
                return new String[]{Double.toString(groupInArea.getSimTime()), Integer.toString(groupInArea.getTotalPedestriansInArea()),
                        Integer.toString(groupInArea.getSizeInArea()), Long.toString(groupInArea.getPedestriansLost()), "NA", "NA"};
            } else {
                return new String[]{Double.toString(groupInArea.getSimTime()), Integer.toString(groupInArea.getTotalPedestriansInArea()),
                        Integer.toString(groupInArea.getSizeInArea()), Long.toString(groupInArea.getPedestriansLost()),
                        Double.toString(groupInArea.getCentroid().get().x), Double.toString(groupInArea.getCentroid().get().y)};
            }
        }
    }

    @Override
    public AttributesProcessor getAttributes() {
        if (super.getAttributes() == null) {
            setAttributes(new AttributesGroupMetaDataProcessor());
        }
        return super.getAttributes();
    }

    /*@Override
    public CompoundObject provide(CompoundObjectBuilder builder) {
        double lastValue = getValue(getLastKey());
        int measurementAreaId = this.measurementArea.getId();
        return builder.rest()
                .add(TraCIDataType.INTEGER) // measurementAreaId
                .add(TraCIDataType.INTEGER) //  timestep of count
                .add(TraCIDataType.DOUBLE) // countInId
                .build(measurementAreaId,
                        getLastKey().getTimestep(),
                        lastValue);
    }*/


}

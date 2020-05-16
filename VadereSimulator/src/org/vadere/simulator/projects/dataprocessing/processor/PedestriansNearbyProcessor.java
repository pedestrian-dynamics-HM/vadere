package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestriansNearbyData;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestriansNearbyIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianNearbyProcessor;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Maxim Dudin
 */

@DataProcessorClass()
public class PedestriansNearbyProcessor extends DataProcessor<TimestepPedestriansNearbyIdKey, PedestriansNearbyData> {
    private double maxDistance; // todo adjustable with json
    private int sampleEveryNthStep;
    private int allowedAbsenceTimestepsIfContactReturns;
    private int minTimespanOfContactTimesteps;



    public PedestriansNearbyProcessor() {
        super("durationTimesteps", "xPath", "yPath");
        setAttributes(new AttributesPedestrianNearbyProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);
        int timeStep = state.getStep();
        if (timeStep % sampleEveryNthStep != 0) {
            return;
        }
        for (Pedestrian ped : peds) {
            // get all Pedestrians with at most maxDistance away
            // this reduces the amount of overlap tests
            VPoint pedPos = ped.getPosition();
            List<DynamicElement> dynElemNneighbours = getDynElementsAtPosition(state.getTopography(), ped.getPosition(), maxDistance);
            List<PedestriansNearbyData> pedsNearby = dynElemNneighbours
                    .parallelStream()
                    .filter(p -> ped.getId() != p.getId())
                    .map(p -> {
                        double xAverage = (ped.getPosition().getX() + p.getPosition().getX())/2.;
                        double yAverage = (ped.getPosition().getY() + p.getPosition().getY())/2.;
                        VPoint pointOfContact = new VPoint(xAverage, yAverage);
                        List<VPoint> trajectory = new ArrayList<>();
                        trajectory.add(pointOfContact);
                        return new PedestriansNearbyData(ped.getId(), p.getId(), sampleEveryNthStep, timeStep, trajectory);})
                    .collect(Collectors.toList());
            pedsNearby.forEach(o -> this.putValue(new TimestepPedestriansNearbyIdKey(timeStep, o.getPedId1(), o.getPedId2()), o));
        }
    }

    @Override
    public void postLoop(final SimulationState state) {
        List<TimestepPedestriansNearbyIdKey> toBeRemoved = new ArrayList<>();
        for (TimestepPedestriansNearbyIdKey key : getKeys()) {
            if (getValue(key).getDurationTimesteps() < minTimespanOfContactTimesteps) {
                toBeRemoved.add(key);
            }
        }
        for (TimestepPedestriansNearbyIdKey toRemove: toBeRemoved) {
            removeKey(toRemove);
        }
    }

    public String[] toStrings(final TimestepPedestriansNearbyIdKey key) {
        return this.hasValue(key) ? this.getValue(key).toStrings() : new String[]{"N/A", "N/A"};
    }

    @Override
    protected void putValue(final TimestepPedestriansNearbyIdKey key, final PedestriansNearbyData value) {
        for (TimestepPedestriansNearbyIdKey alreadyExisting : getKeys()) {
            PedestriansNearbyData currentVal = getValue(alreadyExisting);
            if (key.isAccountedForBy(currentVal)) {
                return;
            } else if (key.isContinuationOf(currentVal, allowedAbsenceTimestepsIfContactReturns)) {
                super.putValue(alreadyExisting, currentVal.getUpdatedData(value, sampleEveryNthStep));
                return;
            }
        }
        super.putValue(key, value);
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesPedestrianNearbyProcessor att = (AttributesPedestrianNearbyProcessor) this.getAttributes();
        maxDistance = att.getMaxDistanceForANearbyPedestrian();
        sampleEveryNthStep = att.getSampleEveryNthStep();
        allowedAbsenceTimestepsIfContactReturns = att.getAllowedAbsenceTimestepsIfContactReturns();
        minTimespanOfContactTimesteps = att.getMinTimespanOfContactTimesteps();
    }

    private List<DynamicElement> getDynElementsAtPosition(final Topography topography, VPoint sourcePosition, double radius) {
        LinkedCellsGrid<DynamicElement> dynElements = topography.getSpatialMap(DynamicElement.class);
        return dynElements.getObjects(sourcePosition, radius);
    }

}

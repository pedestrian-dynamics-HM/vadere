package org.vadere.simulator.models.infection;

import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ThresholdResponseModel extends AbstractDoseResponseModel {

    Topography topography;

    private double threshold;
    //TODO replace threshold by private AttributesThresholdInfectionModel attributesThresholdInfectionModel;


    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        this.domain = domain;
        this.random = random;
        this.topography = domain.getTopography();
        this.attributesAgent = attributesPedestrian;

        // this.attributesThresholdInfectionModel = Model.findAttributes(attributesList, AttributesThresholdInfectionModel.class);
        this.threshold = 999; //
    }

    @Override
    public void preLoop(double simTimeInSec) {

    }

    @Override
    public void postLoop(double simTimeInSec) {
        //TODO check if this works in postLoop
        Collection<Pedestrian> exposedPedestrians = topography.getPedestrianDynamicElements()
                .getElements()
                .stream()
                .filter(pedestrian -> pedestrian.getPathogenAbsorbedLoad() > threshold).collect(Collectors.toSet());

        for (Pedestrian pedestrian : exposedPedestrians) {
            // pedestrian.setInfected(true);
        }
    }

    @Override
    public void update(double simTimeInSec) {

    }
}

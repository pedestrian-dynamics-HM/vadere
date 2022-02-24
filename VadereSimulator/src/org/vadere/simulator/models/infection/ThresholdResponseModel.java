package org.vadere.simulator.models.infection;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.scenarioelements.TopographyController;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;
import org.vadere.state.attributes.models.infection.AttributesExposureModel;
import org.vadere.state.attributes.models.infection.AttributesThresholdResponseModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.ThresholdResponseModelInfectionStatus;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;


@ModelClass
public class ThresholdResponseModel extends AbstractDoseResponseModel {

    Topography topography;

    private AttributesThresholdResponseModel attributesThresholdResponseModel;


    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        this.domain = domain;
        this.random = random;
        this.topography = domain.getTopography();
        this.attributesAgent = attributesPedestrian;
        this.attributesThresholdResponseModel = Model.findAttributes(attributesList, AttributesThresholdResponseModel.class);

        checkIfExposureModelDefined(attributesList);
    }

    /*
     * Check prerequisite for dose response model: Is exposure model defined? Throw error otherwise.
     */
    private void checkIfExposureModelDefined(List<Attributes> attributesList) throws AttributesNotFoundException {
        Set<Attributes> result = attributesList.stream().filter(a -> AttributesExposureModel.class.isAssignableFrom(a.getClass())).collect(Collectors.toSet());
        if (result.size() < 1) {
            throw new RuntimeException(this.getClass() + " requires any exposure model defined by " + AttributesExposureModel.class);
        }
    }

    @Override
    public void preLoop(double simTimeInSec) {
    }

    @Override
    public void postLoop(double simTimeInSec) {
    }

    @Override
    public void update(double simTimeInSec) {

        /*
         * Here, we update the infection status at each simulation step. This could also be done once at the end of the
         * simulation. However, this solution is useful if one wants to know when/where the threshold is exceeded.
         */
        Collection<Pedestrian> exposedPedestrians = topography.getPedestrianDynamicElements()
                .getElements()
                .stream()
                .filter(pedestrian -> pedestrian.getDegreeOfExposure() >= attributesThresholdResponseModel.getExposureToInfectedThreshold()).collect(Collectors.toSet());
        for (Pedestrian pedestrian : exposedPedestrians) {
            pedestrian.setProbabilityOfInfectionToMax();
        }
    }

    @Override
    public Agent sourceControllerEvent(SourceController controller, double simTimeInSec, Agent scenarioElement) {
        Pedestrian ped = (Pedestrian) scenarioElement;
        ped.setInfectionStatus(new ThresholdResponseModelInfectionStatus());
        return ped;
    }

    @Override
    protected Pedestrian topographyControllerEvent(TopographyController topographyController, double simTimeInSec, Agent agent) {
        Pedestrian pedestrian = (Pedestrian) agent;
        pedestrian.setInfectionStatus(new ThresholdResponseModelInfectionStatus());
        return pedestrian;
    }
}

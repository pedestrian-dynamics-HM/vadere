package org.vadere.simulator.models.infection;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.scenarioelements.TopographyController;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.infection.AttributesThresholdResponseModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.ExposureModelHealthStatus;
import org.vadere.state.health.ThresholdResponseModelInfectionStatus;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


/**
 * ThresholdResponseModel describes a <code>Pedestrian</code>'s probability of
 * infection, more precisely its {@link ThresholdResponseModelInfectionStatus}.
 * <p>
 *     It can be included in the simulation by adding it to the list of
 *     <code>submodels</code> in the scenario file. This requires that an
 *     exposure model is defined.
 * </p>
 * <p>
 *     The probability of infection is 0 by default but set to 1 if the
 *     <code>Pedestrian</code>'s degree of exposure
 *     {@link ExposureModelHealthStatus} reaches or exceeds a user-defined
 *     threshold. The threshold is defined by
 *     {@link AttributesThresholdResponseModel}.
 * </p>
 */
@ModelClass
public class ThresholdResponseModel extends AbstractDoseResponseModel {

    Topography topography;

    protected AttributesThresholdResponseModel attributesThresholdResponseModel;


    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        this.domain = domain;
        this.random = random;
        this.topography = domain.getTopography();
        this.attributesAgent = attributesPedestrian;
        this.attributesThresholdResponseModel = Model.findAttributes(attributesList, AttributesThresholdResponseModel.class);

        checkIfExposureModelDefined(attributesList);
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

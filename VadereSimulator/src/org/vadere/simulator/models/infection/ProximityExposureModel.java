package org.vadere.simulator.models.infection;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.scenarioelements.TopographyController;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.infection.AttributesExposureModelSourceParameters;
import org.vadere.state.attributes.models.infection.AttributesProximityExposureModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.BasicExposureModelHealthStatus;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * ProximityExposureModel describes the degree of exposure of
 * <code>Pedestrian</code>s to another infectious one simply by their mutual
 * distance.
 * <p>
 * </p>
 * <p> <code>ProximityExposureModel</code> contains the logic, that is:
 * <ul>
 *     <li>Each pedestrian obtains a {@link BasicExposureModelHealthStatus
 *     health status}
 *     after being inserted into the topography.</li>
 *     <li>Any pedestrian that approaches an infectious pedestrian so that the
 *     mutual distance falls below a defined threshold becomes exposed. The
 *     threshold is defined in {@link AttributesProximityExposureModel}.</li>
 * </ul>
 */
@ModelClass
public class ProximityExposureModel extends AbstractExposureModel {

    /*
     * defines the maximum possible degree of exposure a pedestrian can adopt
     */
    protected static final double MAX_DEG_OF_EXPOSURE = 1;

    Topography topography;

    AttributesProximityExposureModel attributesProximityExposureModel;

    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        this.domain = domain;
        this.random = random;
        this.attributesAgent = attributesPedestrian;
        this.attributesProximityExposureModel = Model.findAttributes(attributesList, AttributesProximityExposureModel.class);
        this.topography = domain.getTopography();
    }

    @Override
    public void preLoop(double simTimeInSec) {

    }

    @Override
    public void postLoop(double simTimeInSec) {

    }

    @Override
    public void update(double simTimeInSec) {
        Collection<Pedestrian> pedestrians;
        pedestrians = topography.getPedestrianDynamicElements().getElements().stream().collect(Collectors.toSet());

        Collection<Pedestrian> infectiousPedestrians = pedestrians.stream()
                .filter(Pedestrian::isInfectious).collect(Collectors.toSet());

        if (!infectiousPedestrians.isEmpty()) {
            for (Pedestrian infectiousPedestrian : infectiousPedestrians) {
                Collection<Pedestrian> exposedPedestrians = getPedestriansNearbyInfectiousPedestrian(pedestrians, infectiousPedestrian);

                for (Pedestrian pedestrian : exposedPedestrians) {
                    updatePedestrianDegreeOfExposure(pedestrian, MAX_DEG_OF_EXPOSURE);
                }
            }
        }

    }

    @NotNull
    private Collection<Pedestrian> getPedestriansNearbyInfectiousPedestrian(Collection<Pedestrian> pedestrians, Pedestrian infectiousPedestrian) {
        VPoint position = infectiousPedestrian.getPosition();
        VShape areaOfExposure = new VCircle(position, attributesProximityExposureModel.getExposureRadius());

        Collection<Pedestrian> exposedPedestrians = pedestrians
                .stream()
                .filter(p -> (areaOfExposure.contains(p.getPosition())) && p.getId() != infectiousPedestrian.getId()).collect(Collectors.toSet());
        return exposedPedestrians;
    }

    /**
     * This simple approach allows only 0 or {@link #MAX_DEG_OF_EXPOSURE}. The degree of exposure is increased only
     * once.
     */
    @Override
    public void updatePedestrianDegreeOfExposure(Pedestrian pedestrian, double degreeOfExposure) {
        pedestrian.setDegreeOfExposure(degreeOfExposure);
    }

    @Override
    public Agent sourceControllerEvent(SourceController controller, double simTimeInSec, Agent scenarioElement) {
        AttributesExposureModelSourceParameters sourceParameters = defineSourceParameters(controller, attributesProximityExposureModel);

        Pedestrian ped = (Pedestrian) scenarioElement;
        ped.setHealthStatus(new BasicExposureModelHealthStatus());
        ped.setInfectious(sourceParameters.isInfectious());
        return ped;
    }

    @Override
    public Pedestrian topographyControllerEvent(TopographyController topographyController, double simTimeInSec, Agent agent) {
        Pedestrian pedestrian = (Pedestrian) agent;

        pedestrian.setHealthStatus(new BasicExposureModelHealthStatus());

        if (attributesProximityExposureModel.getInfectiousPedestrianIdsNoSource().contains(agent.getId())) {
            pedestrian.setInfectious(true);
        }

        return pedestrian;
    }

}

package org.vadere.simulator.models.infection;

import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.scenarioelements.TopographyController;
import org.vadere.simulator.control.simulation.ControllerProvider;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.models.infection.AttributesExposureModel;
import org.vadere.state.attributes.models.infection.AttributesExposureModelSourceParameters;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.ExposureModelHealthStatus;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.logging.Logger;

import java.util.Optional;
import java.util.Random;

/**
 * AbstractExposureModel is the abstract base class for all exposure models which
 * allow to describe a <code>Pedestrian</code>'s degree of exposure, more
 * precisely, its {@link ExposureModelHealthStatus health status}.
 * <p>
 *     An exposure model can be included in the simulation by defining the
 *     desired model in the scenario file in the list of <code>submodels</code>.
 * </p>
 */
public abstract class AbstractExposureModel implements Model {

    protected static Logger logger = Logger.getLogger(AbstractExposureModel.class);

    // this random provider everywhere to keep simulation reproducible
    protected Random random;
    protected Domain domain;
    protected AttributesAgent attributesAgent;


    abstract void updatePedestrianDegreeOfExposure(final Pedestrian pedestrian, double degreeOfExposure);

    @Override
    public void registerToScenarioElementControllerEvents(ControllerProvider controllerProvider) {
        /*
         * This will be called *after* a pedestrian is inserted into the topography by the given SourceController.
         * Change model state on Agent here.
         * ControllerProvider could also be handled by initialize method (this requires changes in all models)
         */
        for (var controller : controllerProvider.getSourceControllers()){
            controller.register(this::sourceControllerEvent);
        }

        controllerProvider.getTopographyController().register(this::topographyControllerEvent);
    }


    public AttributesExposureModelSourceParameters defineSourceParameters(SourceController controller, AttributesExposureModel attributes) {
        int sourceId = controller.getSourceId();
        int defaultSourceId = -1;
        Optional<AttributesExposureModelSourceParameters> sourceParameters = attributes
                .getExposureModelSourceParameters().stream().filter(s -> s.getSourceId() == sourceId).findFirst();

        // if sourceId not set by user, check if the user has defined default attributes by setting sourceId = -1
        if (sourceParameters.isEmpty()) {
            sourceParameters = attributes.getExposureModelSourceParameters().stream().filter(s -> s.getSourceId() == defaultSourceId).findFirst();

            // if no user defined default values: use attributesAirTransmissionModel default values
            if (sourceParameters.isPresent()) {
                logger.infof(">>>>>>>>>>>defineSourceParameters: sourceId %d not set explicitly exposureModelSourceParameters. Source uses default exposureModelSourceParameters defined for sourceId: %d", sourceId, defaultSourceId);
            } else {
                logger.errorf(">>>>>>>>>>>defineSourceParameters: sourceId %d is not set in exposureModelSourceParameters", sourceId);
            }
        }
        return sourceParameters.get();
    }

    /**
     * Assures that each pedestrian that is spawned by sources obtains properties related to
     * {@link AttributesExposureModel} when pedestrian is added to topography.
     */
    protected abstract Agent sourceControllerEvent(SourceController controller, double simTimeInSec, Agent scenarioElement);


    /**
     * Assures that each pedestrian that is directly set into the topography obtains properties
     * related to {@link AttributesExposureModel}.
     */
    protected abstract Pedestrian topographyControllerEvent(TopographyController topographyController, double simtimeInSec, Agent agent);
}

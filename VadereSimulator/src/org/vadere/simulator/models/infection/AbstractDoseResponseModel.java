package org.vadere.simulator.models.infection;

import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.scenarioelements.TopographyController;
import org.vadere.simulator.control.simulation.ControllerProvider;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.models.infection.AttributesDoseResponseModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.logging.Logger;

import java.util.Random;

public abstract class AbstractDoseResponseModel implements Model {

    protected static Logger logger = Logger.getLogger(AbstractDoseResponseModel.class);

    // this random provider everywhere to keep simulation reproducible
    protected Random random;
    protected Domain domain;
    protected AttributesAgent attributesAgent;

    /*
     * This will be called  *after* a pedestrian is inserted into the topography by the given SourceController.
     * Change model state on Agent here
     */
    @Override
    public void registerToScenarioElementControllerEvents(ControllerProvider controllerProvider) {
        for (var controller : controllerProvider.getSourceControllers()){
            controller.register(this::sourceControllerEvent);
        }
        controllerProvider.getTopographyController().register(this::topographyControllerEvent);
    }

    /**
     * Assures that each pedestrian that is spawned by sources obtains properties related to
     * {@link AttributesDoseResponseModel} when pedestrian is added to topography.
     */
    protected abstract Agent sourceControllerEvent(SourceController controller, double simTimeInSec, Agent scenarioElement);

    /**
     * Assures that each pedestrian that is directly set into the topography obtains properties
     * related to {@link AttributesDoseResponseModel}.
     */
    protected abstract Pedestrian topographyControllerEvent(TopographyController topographyController, double simTimeInSec, Agent agent);
}

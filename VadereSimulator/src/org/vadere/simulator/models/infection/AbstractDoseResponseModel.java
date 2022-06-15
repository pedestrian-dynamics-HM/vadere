package org.vadere.simulator.models.infection;

import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.scenarioelements.TopographyController;
import org.vadere.simulator.control.simulation.ControllerProvider;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;
import org.vadere.state.attributes.models.infection.AttributesDoseResponseModel;
import org.vadere.state.attributes.models.infection.AttributesExposureModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.DoseResponseModelInfectionStatus;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.logging.Logger;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AbstractDoseResponseModel is the abstract base class for all dose response
 * models which allow to describe a <code>Pedestrian</code>'s probability of
 * infection, more precisely its {@link DoseResponseModelInfectionStatus}.
 * <p>
 *     A dose response model can be included in the simulation by defining the
 * desired model in the scenario file in the list of <code>submodels</code>.
 * </p>
 * <p>
 * Any dose response model requires that an exposure model is defined.
 * </p>
 *
 * @see AbstractExposureModel
 */
public abstract class AbstractDoseResponseModel implements Model {

    protected static Logger logger = Logger.getLogger(AbstractDoseResponseModel.class);

    // this random provider everywhere to keep simulation reproducible
    protected Random random;
    protected Domain domain;
    protected AttributesAgent attributesAgent;

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

    /**
     * Checks prerequisite for dose response model.
     *
     * @throws AttributesNotFoundException if no exposure model defined.
     */
    protected void checkIfExposureModelDefined(List<Attributes> attributesList) throws AttributesNotFoundException {
        Set<Attributes> result = attributesList.stream().filter(a -> AttributesExposureModel.class.isAssignableFrom(a.getClass())).collect(Collectors.toSet());
        if (result.size() < 1) {
            throw new RuntimeException(this.getClass() + " requires any exposure model defined by " + AttributesExposureModel.class);
        }
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

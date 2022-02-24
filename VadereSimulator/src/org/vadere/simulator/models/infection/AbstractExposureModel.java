package org.vadere.simulator.models.infection;

import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.models.infection.AttributesExposureModel;
import org.vadere.state.attributes.models.infection.AttributesExposureModelSourceParameters;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.logging.Logger;

import java.util.Optional;
import java.util.Random;

public abstract class AbstractExposureModel implements ExposureModel {

    // add default implementations and shared fields here to keep ExposureModel interface clean

    protected static Logger logger = Logger.getLogger(AbstractExposureModel.class);

    // this random provider everywhere to keep simulation reproducible
    protected Random random;
    protected Domain domain;
    protected AttributesAgent attributesAgent;

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

}

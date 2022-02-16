package org.vadere.simulator.models.infection;

import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.logging.Logger;

import java.util.Random;

public abstract class AbstractExposureModel implements ExposureModel {

    // add default implementations and shared fields here to keep ExposureModel interface clean

    protected static Logger logger = Logger.getLogger(AbstractExposureModel.class);

    // this random provider everywhere to keep simulation reproducible
    protected Random random;
    protected Domain domain;
    protected AttributesAgent attributesAgent;

}

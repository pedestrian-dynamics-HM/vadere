package org.vadere.simulator.models.sir;

import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.logging.Logger;

import java.util.Random;

// TODO remove class AbstractSirModel if no additional models similar to TransmissionModel are implemented
public abstract class AbstractSirModel implements SirModel {

	// add default implementations and shared fields here to keep SirModel interface clean

	protected static Logger logger = Logger.getLogger(AbstractSirModel.class);

	// this random provider everywhere to keep simulation reproducible
	protected Random random;
	protected Domain domain;
	protected AttributesAgent attributesAgent;

}

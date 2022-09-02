package org.vadere.simulator.models.osm;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.*;
import org.vadere.simulator.projects.Domain;
import org.vadere.util.Attributes;
import org.vadere.state.attributes.models.AttributesCA;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.logging.Logger;

import java.util.*;

/** Introduces to allow to load CA model presets in GUI
 * @author hm-mgoedel
 */

@ModelClass(isMainModel = true)
public class CellularAutomaton extends OptimalStepsModel {

	private final static Logger logger = Logger.getLogger(CellularAutomaton.class);



	public CellularAutomaton() {
		super();
	}

	@Override
	public void initialize(List<Attributes> modelAttributesList, Domain domain,
						   AttributesAgent attributesPedestrian, Random random) {

		logger.debug("initialize CA");

		initialize(modelAttributesList, domain, attributesPedestrian, random,
				Model.findAttributes(modelAttributesList, AttributesCA.class), logger);
		}
}


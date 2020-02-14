package org.vadere.simulator.models.osm;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.*;
import org.vadere.simulator.models.osm.optimization.*;
import org.vadere.simulator.models.osm.updateScheme.UpdateSchemeOSM;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesCA;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;

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


package org.vadere.simulator.models.osm;

import java.util.*;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.*;
import org.vadere.simulator.models.osm.optimization.*;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesCA;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.logging.Logger;

/**
 * Introduces to allow to load CA model presets in GUI
 *
 * @author hm-mgoedel
 */
@ModelClass(isMainModel = true)
public class CellularAutomaton extends OptimalStepsModel {

  private static final Logger logger = Logger.getLogger(CellularAutomaton.class);

  public CellularAutomaton() {
    super();
  }

  @Override
  public void initialize(
      List<Attributes> modelAttributesList,
      Domain domain,
      AttributesAgent attributesPedestrian,
      Random random) {

    logger.debug("initialize CA");

    initialize(
        modelAttributesList,
        domain,
        attributesPedestrian,
        random,
        Model.findAttributes(modelAttributesList, AttributesCA.class),
        logger);
  }
}

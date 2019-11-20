package org.vadere.simulator.control.factory;

import org.vadere.simulator.control.scenarioelements.SingleSourceController;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;

import java.util.Random;

public class SingleSourceControllerFactory extends SourceControllerFactory {

	@Override
	public SourceController create(Topography scenario, Source source,
								   DynamicElementFactory dynamicElementFactory,
								   AttributesDynamicElement attributesDynamicElement,
								   Random random) {

		return new SingleSourceController(scenario, source, dynamicElementFactory, attributesDynamicElement, random);
	}
}

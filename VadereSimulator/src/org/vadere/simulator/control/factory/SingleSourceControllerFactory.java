package org.vadere.simulator.control.factory;

import org.vadere.simulator.control.SingleSourceController;
import org.vadere.simulator.control.SingleSourceUniformDistributedController;
import org.vadere.simulator.control.SourceController;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Random;

public class SingleSourceControllerFactory extends SourceControllerFactory {

	@Override
	public SourceController create(Topography scenario, Source source,
								   DynamicElementFactory dynamicElementFactory,
								   AttributesDynamicElement attributesDynamicElement,
								   Random random) {

		if(source.getAttributes().getShape() instanceof VRectangle && source.getAttributes().isSpawnAtRandomPositions())  {
			return new SingleSourceUniformDistributedController(scenario, source, dynamicElementFactory, attributesDynamicElement, random);
		} else {
			return new SingleSourceController(scenario, source, dynamicElementFactory, attributesDynamicElement, random);
		}
	}
}

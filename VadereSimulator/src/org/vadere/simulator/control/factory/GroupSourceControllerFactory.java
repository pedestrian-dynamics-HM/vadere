package org.vadere.simulator.control.factory;

import org.vadere.simulator.control.GroupSourceController;
import org.vadere.simulator.control.SourceController;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.simulator.models.groups.GroupModel;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;

import java.util.Random;

public class GroupSourceControllerFactory extends SourceControllerFactory {

	private final GroupModel groupModel;

	public GroupSourceControllerFactory(GroupModel groupModel) {
		this.groupModel = groupModel;
	}

	@Override
	public SourceController create(Topography scenario, Source source,
								   DynamicElementFactory dynamicElementFactory,
								   AttributesDynamicElement attributesDynamicElement,
								   Random random) {
		groupModel.initializeGroupFactory(source.getId(), source.getAttributes().getGroupSizeDistribution());
		return new GroupSourceController(scenario, source, dynamicElementFactory, attributesDynamicElement, random, groupModel);
	}
}
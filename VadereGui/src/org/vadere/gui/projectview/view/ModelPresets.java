package org.vadere.gui.projectview.view;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.gnm.GradientNavigationModel;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.simulator.models.ovm.OptimalVelocityModel;
import org.vadere.simulator.models.sfm.SocialForceModel;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.ModelDefinition;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesGNM;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.models.AttributesOVM;
import org.vadere.state.attributes.models.AttributesPotentialCompact;
import org.vadere.state.attributes.models.AttributesPotentialGNM;
import org.vadere.state.attributes.models.AttributesPotentialSFM;
import org.vadere.state.attributes.models.AttributesSFM;

/**
 * Singleton for model presets.
 * 
 */
public class ModelPresets {

	private static ModelPresets instance;
	private final List<ModelDefinition> modelDefinitionPresets = new LinkedList<>();

	public static List<ModelDefinition> getPresets() {
		return getInstance().modelDefinitionPresets;
	}

	private ModelPresets() {

		final List<Class<? extends Attributes>> list = new ArrayList<>();

		// ADD MODEL PRESETS BELOW

		// Optimal Steps Model
		list.clear();
		list.add(AttributesOSM.class);
		list.add(AttributesPotentialCompact.class);
		list.add(AttributesFloorField.class);
		registerModelPreset(OptimalStepsModel.class, list);

		list.clear();
		list.add(AttributesGNM.class);
		list.add(AttributesPotentialGNM.class);
		list.add(AttributesFloorField.class);
		registerModelPreset(GradientNavigationModel.class, list);

		list.clear();
		list.add(AttributesSFM.class);
		list.add(AttributesPotentialSFM.class);
		list.add(AttributesFloorField.class);
		registerModelPreset(SocialForceModel.class, list);
		
		list.clear();
		list.add(AttributesOVM.class);
		registerModelPreset(OptimalVelocityModel.class, list);

		// list.clear();
		// list.add(...);
		// registerModelPreset(MyModel.class, list);
	}

	private void registerModelPreset(Class<? extends MainModel> mainModelClass,
			List<Class<? extends Attributes>> attributesClasses) {

		ModelDefinition definition = new ModelDefinition(mainModelClass.getName(), null);
		definition.createAndSetDefaultAttributes(attributesClasses);
		modelDefinitionPresets.add(definition);
	}

	private static ModelPresets getInstance() {
		if (instance == null) {
			instance = new ModelPresets();
		}
		return instance;
	}

}

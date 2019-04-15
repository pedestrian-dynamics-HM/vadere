package org.vadere.simulator.projects.migration.incident;

import java.util.HashMap;
import java.util.Map;

import org.vadere.simulator.models.gnm.GradientNavigationModel;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.simulator.models.sfm.SocialForceModel;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.attributes.models.AttributesBMM;
import org.vadere.state.attributes.models.AttributesCGM;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesGFM;
import org.vadere.state.attributes.models.AttributesGNM;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.models.AttributesOVM;
import org.vadere.state.attributes.models.AttributesParticles;
import org.vadere.state.attributes.models.AttributesPotentialCompact;
import org.vadere.state.attributes.models.AttributesPotentialCompactSoftshell;
import org.vadere.state.attributes.models.AttributesPotentialGNM;
import org.vadere.state.attributes.models.AttributesPotentialOSM;
import org.vadere.state.attributes.models.AttributesPotentialParticles;
import org.vadere.state.attributes.models.AttributesPotentialRingExperiment;
import org.vadere.state.attributes.models.AttributesPotentialSFM;
import org.vadere.state.attributes.models.AttributesQueuingGame;
import org.vadere.state.attributes.models.AttributesReynolds;
import org.vadere.state.attributes.models.AttributesSFM;
import org.vadere.state.attributes.scenario.AttributesCar;

public class LookupTables {

	public final static Map<String, String> version0to1_ModelRenaming = new HashMap<>();
	static {
		version0to1_ModelRenaming.put("BEHAVIOURAL_HEURISTICS_MODEL", AttributesBHM.class.getName());
		version0to1_ModelRenaming.put("BIOMECHANICS_MODEL", AttributesBMM.class.getName());
		version0to1_ModelRenaming.put("CAR_ATTRIBUTES", AttributesCar.class.getName());
		version0to1_ModelRenaming.put("CENTROID_GROUP_MODEL", AttributesCGM.class.getName());
		version0to1_ModelRenaming.put("FLOORFIELD", AttributesFloorField.class.getName());
		version0to1_ModelRenaming.put("FREDERIX_MODEL", AttributesParticles.class.getName());
		version0to1_ModelRenaming.put("GRADIENT_NAVIGATION_MODEL", AttributesGNM.class.getName());
		version0to1_ModelRenaming.put("GRANULAR_FLOW_MODEL", AttributesGFM.class.getName());
		version0to1_ModelRenaming.put("OBSTACLE_POTENTIAL_GNM", AttributesPotentialGNM.class.getName());
		version0to1_ModelRenaming.put("OBSTACLE_POTENTIAL_OSM", AttributesPotentialOSM.class.getName());
		version0to1_ModelRenaming.put("OBSTACLE_POTENTIAL_PARTICLES", AttributesPotentialParticles.class.getName());
		version0to1_ModelRenaming.put("OBSTACLE_POTENTIAL_RING_EXPERIMENT", AttributesPotentialRingExperiment.class.getName());
		version0to1_ModelRenaming.put("OBSTACLE_POTENTIAL_SFM", AttributesPotentialSFM.class.getName());
		version0to1_ModelRenaming.put("OPTIMAL_STEPS_MODEL", AttributesOSM.class.getName());
		version0to1_ModelRenaming.put("OPTIMAL_VELOCITY_MODEL", AttributesOVM.class.getName());
		version0to1_ModelRenaming.put("PEDESTRIAN_POTENTIAL_GNM", AttributesPotentialGNM.class.getName());
		version0to1_ModelRenaming.put("PEDESTRIAN_POTENTIAL_OSM", AttributesPotentialOSM.class.getName());
		version0to1_ModelRenaming.put("PEDESTRIAN_POTENTIAL_PARTICLES", AttributesPotentialParticles.class.getName());
		version0to1_ModelRenaming.put("PEDESTRIAN_POTENTIAL_SFM", AttributesPotentialSFM.class.getName());
		version0to1_ModelRenaming.put("POTENTIAL_COMPACT_SUPPORT", AttributesPotentialCompact.class.getName());
		version0to1_ModelRenaming.put("POTENTIAL_COMPACT_SUPPORT_SOFTSHELL", AttributesPotentialCompactSoftshell.class.getName());
		version0to1_ModelRenaming.put("QUEUEING_GAME", AttributesQueuingGame.class.getName());
		version0to1_ModelRenaming.put("REYNOLDS_STEERING_MODEL", AttributesReynolds.class.getName());
		version0to1_ModelRenaming.put("SOCIAL_FORCE_MODEL", AttributesSFM.class.getName());
		// version0to1_ModelRenaming.put("SOCIAL_IDENTITY_MODEL_APPLICATION", AttributesSIMA.class.getName());
		version0to1_ModelRenaming.put("TARGET_POTENTIAL_RING_EXPERIMENT",
				AttributesPotentialRingExperiment.class.getName());
	}

	public static final Map<String, String> version0to1_IdentifyingMainModel = new HashMap<>();
	static {
		version0to1_IdentifyingMainModel.put(AttributesOSM.class.getName(), OptimalStepsModel.class.getName());
		version0to1_IdentifyingMainModel.put(AttributesGNM.class.getName(), GradientNavigationModel.class.getName());
		version0to1_IdentifyingMainModel.put(AttributesSFM.class.getName(), SocialForceModel.class.getName());
	}


}

package org.vadere.simulator.projects.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.simulator.projects.migration.MigrationAssistant.Version;
import org.vadere.simulator.projects.migration.incidents.AddTextNodeIncident;
import org.vadere.simulator.projects.migration.incidents.DeletionIncident;
import org.vadere.simulator.projects.migration.incidents.Incident;
import org.vadere.simulator.projects.migration.incidents.MissingMainModelIncident;
import org.vadere.simulator.projects.migration.incidents.RelocationIncident;
import org.vadere.simulator.projects.migration.incidents.RenameInArrayIncident;
import org.vadere.simulator.projects.migration.incidents.RenameIncident;
import org.vadere.simulator.projects.migration.incidents.specialized.AttributesPotentialCompactVSosmIncident;
import org.vadere.simulator.projects.migration.incidents.specialized.MoveSpawnDelayIntoDistributionParametersIncident;

public class IncidentDatabase {

	private Map<Version, List<Incident>> incidents = new LinkedHashMap<>();

	private IncidentDatabase() {

		/*
		A list of incidents always marks the possible incidents from one version to another.
		The key in the Map represents the departure-version. An incident that's added to the
		Version.NOT_A_RELEASE list for instance, get's checked for applicability when making
		the migration <from> Version.NOT_A_RELEASE <to> Version.V0_1
		 */

		// - - - - - - - - - - - - "not a release" to "0.1" - - - - - - - - - - - -

		incidents.put(Version.NOT_A_RELEASE, new ArrayList<>());

		incidents.get(Version.NOT_A_RELEASE).add(new RelocationIncident(
				"finishTime",
				path("vadere", "topography", "attributes"),
				path("vadere", "attributesSimulation")));

		incidents.get(Version.NOT_A_RELEASE).add(new RelocationIncident(
				"attributesPedestrian",
				path("vadere"),
				path("vadere", "topography")));

		incidents.get(Version.NOT_A_RELEASE).add(new DeletionIncident(
				path("vadere", "topography", "pedestrians")));

		incidents.get(Version.NOT_A_RELEASE).add(new RenameInArrayIncident(
				path("vadere", "topography", "dynamicElements"),
				"nextTargetListPosition",
				"nextTargetListIndex"));

		for (String oldName : LookupTables.version0to1_ModelRenaming.keySet()) {
			String newName = LookupTables.version0to1_ModelRenaming.get(oldName);
			incidents.get(Version.NOT_A_RELEASE).add(new RenameIncident(
					path("vadere", "attributesModel", oldName), newName));
		}

		incidents.get(Version.NOT_A_RELEASE).add(new MissingMainModelIncident( // must come AFTER the model renaming that was done in the loop before
				path("vadere"),
				JsonConverter.MAIN_MODEL_KEY,
				path("vadere", "attributesModel")));

		incidents.get(Version.NOT_A_RELEASE).add(new AddTextNodeIncident(
				path(),
				"description", ""));

		incidents.get(Version.NOT_A_RELEASE).add(new AttributesPotentialCompactVSosmIncident()); // requested by Bene
		incidents.get(Version.NOT_A_RELEASE).add(new MoveSpawnDelayIntoDistributionParametersIncident()); // requested by Jakob

		incidents.get(Version.NOT_A_RELEASE).add(new RenameIncident(path("vadere"), JsonConverter.SCENARIO_KEY));

		// - - - - - - - - - - - - "0.1" to "?" - - - - - - - - - - - -

		//incidents.put(Version.V0_1, new ArrayList<>());
		//incidents.get(Version.V0_1).add(...
	}

	public List<Incident> getPossibleIncidentsFor(Version version) {
		if (incidents.containsKey(version)) {
			return incidents.get(version);
		}
		return new ArrayList<>();
	}

	public static List<String> path(String... entries) {
		return Arrays.asList(entries);
	}

	private static IncidentDatabase instance = null;

	public static IncidentDatabase getInstance() {
		if (instance == null) {
			instance = new IncidentDatabase();
		}
		return instance;
	}

}



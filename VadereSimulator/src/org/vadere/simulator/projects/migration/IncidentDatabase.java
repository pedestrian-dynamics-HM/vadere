package org.vadere.simulator.projects.migration;

import java.util.*;

import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.simulator.projects.migration.incidents.*;
import org.vadere.simulator.projects.migration.incidents.specialized.AttributesPotentialCompactVSosmIncident;

public class IncidentDatabase {

	private Map<Integer, List<Incident>> versionIncidents = new LinkedHashMap<>();

	private IncidentDatabase() {

		// - - - - - - - - - - - - 0 ("not a release") to 1 ("0.1") - - - - - - - - - - - -

		List<Incident> version0to1 = new ArrayList<>();
		versionIncidents.put(0, version0to1);

		version0to1.add(new RelocationIncident(
				"finishTime",
				path("vadere", "topography", "attributes"),
				path("vadere", "attributesSimulation")));

		version0to1.add(new RelocationIncident(
				"attributesPedestrian",
				path("vadere"),
				path("vadere", "topography")));

		version0to1.add(new DeletionIncident(
				path("vadere", "topography", "pedestrians")));

		version0to1.add(new RenameInArrayIncident(
				path("vadere", "topography", "dynamicElements"),
				"nextTargetListPosition",
				"nextTargetListIndex"));

		LookupTables.version0to1_ModelRenaming.forEach((oldName, newName) -> version0to1.add(new RenameIncident(
				path("vadere", "attributesModel", oldName),
				newName)));

		version0to1.add(new MissingMainModelIncident( // must come AFTER the model renaming that was done in the loop before
				path("vadere"),
				JsonConverter.MAIN_MODEL_KEY,
				path("vadere", "attributesModel")));

		version0to1.add(new AddTextNodeIncident(
				path(),
				"description", ""));

		version0to1.add(new AttributesPotentialCompactVSosmIncident());

		// - - - - - - - - - - - - 1 ("0.1") to 2 (?) - - - - - - - - - - - -
		// List<Incident> version1to2 = new ArrayList<>();
		// versionIncidents.put(1, version1to2);
		// ...
	}

	public List<Incident> getPossibleIncidentsFor(int version) {
		return versionIncidents.get(version);
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



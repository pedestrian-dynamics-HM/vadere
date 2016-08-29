package org.vadere.simulator.projects.migration;

import java.util.*;

import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.simulator.projects.migration.incidents.*;
import org.vadere.simulator.projects.migration.incidents.specialized.AttributesPotentialCompactVSosmIncident;

public class IncidentDatabase {

	private Map<Integer, List<Incident>> versionIncidents = new LinkedHashMap<>();

	private IncidentDatabase() {

		List<Incident> incidents;

		// - - - - - - - - - - - - "not a release" to "0.1" - - - - - - - - - - - -

		incidents = new ArrayList<>();
		versionIncidents.put(0, incidents);

		incidents.add(new RelocationIncident(
				"finishTime",
				path("vadere", "topography", "attributes"),
				path("vadere", "attributesSimulation")));

		incidents.add(new RelocationIncident(
				"attributesPedestrian",
				path("vadere"),
				path("vadere", "topography")));

		incidents.add(new DeletionIncident(
				path("vadere", "topography", "pedestrians")));

		incidents.add(new RenameInArrayIncident(
				path("vadere", "topography", "dynamicElements"),
				"nextTargetListPosition",
				"nextTargetListIndex"));

		for (String oldName : LookupTables.version0to1_ModelRenaming.keySet()) {
			String newName = LookupTables.version0to1_ModelRenaming.get(oldName);
			incidents.add(new RenameIncident(
					path("vadere", "attributesModel", oldName), newName));
		}

		incidents.add(new MissingMainModelIncident( // must come AFTER the model renaming that was done in the loop before
				path("vadere"),
				JsonConverter.MAIN_MODEL_KEY,
				path("vadere", "attributesModel")));

		incidents.add(new AddTextNodeIncident(
				path(),
				"description", ""));

		incidents.add(new AttributesPotentialCompactVSosmIncident());

		// - - - - - - - - - - - - "0.1" to "?" - - - - - - - - - - - -

		incidents = new ArrayList<>();
		versionIncidents.put(1, incidents);

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



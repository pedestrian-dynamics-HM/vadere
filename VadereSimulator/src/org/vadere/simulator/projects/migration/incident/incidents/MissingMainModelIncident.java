package org.vadere.simulator.projects.migration.incident.incidents;

import java.util.List;

import org.vadere.simulator.projects.migration.incident.Tree;
import org.vadere.simulator.projects.migration.incident.LookupTables;
import org.vadere.simulator.projects.migration.MigrationException;

public class MissingMainModelIncident extends Incident {

	private List<String> path;
	private List<String> fullPath;
	private String mainModelKey;
	private List<String> pathToModels;

	public MissingMainModelIncident(List<String> path, String mainModelKey, List<String> pathToModels) {
		this.path = path;
		this.fullPath = clone(path);
		this.fullPath.add(mainModelKey);
		this.mainModelKey = mainModelKey;
		this.pathToModels = pathToModels;
	}

	@Override
	public boolean applies(Tree graph) {
		return !graph.pathExists(fullPath);
	}

	@Override
	public void resolve(Tree graph, StringBuilder log) throws MigrationException {
		super.stillApplies(graph);

		String identifiedMainModel = null;
		for (String modelIdentifier : graph.getKeysOfChildren(pathToModels)) {
			String mainModelMatch = LookupTables.version0to1_IdentifyingMainModel.get(modelIdentifier);
			if (mainModelMatch != null) {
				if (identifiedMainModel == null) {
					identifiedMainModel = mainModelMatch;
					graph.createTextNode(path, mainModelKey, mainModelMatch);
				} else {
					throw new MigrationException(this,
							"can't automatically determine the mainModel - more than one mainModel-suitable model is present");
				}
			}
		}

		if (identifiedMainModel == null)
			throw new MigrationException(this,
					"couldn't automatically determine the mainModel based on the present models - update the \"version0to1_IdentifyingMainModel\" lookup-table if there is actually a mainModel-suitable model present");

		log.append("\t- create [mainModel] node \"" + identifiedMainModel + "\" under node " + graph.pathToString(path)
				+ "\n");
	}

}

package org.vadere.simulator.projects.migration.jsontranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;

public abstract class SimpleJsonTransformation extends AbstractJsonTransformation {

    private Version previousVersion;
    private Version targetVersion;

    public SimpleJsonTransformation(Version targetVersion) {
        super();
        this.targetVersion = targetVersion;
        this.previousVersion = targetVersion.previousVersion();
        //todo do something with the version.
    }

    /**
     * Nothing to do here. All transformations will be manged as postHooks
     * @param node      JsonNode representing the scenario file
     * @return          as input
     */
    @Override
    public JsonNode applyTransformation(JsonNode node) throws MigrationException {
        JsonNode ret = node;
        ret = setVersionFromTo(node, previousVersion, targetVersion);
        return ret;
    }
}

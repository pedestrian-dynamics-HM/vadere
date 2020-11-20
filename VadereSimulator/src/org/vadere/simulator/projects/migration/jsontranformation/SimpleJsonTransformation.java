package org.vadere.simulator.projects.migration.jsontranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;

public abstract class SimpleJsonTransformation extends AbstractJsonTransformation {

    private final Version previousVersion;
    private final Version targetVersion;

    public SimpleJsonTransformation(final Version targetVersion) {
        super();
        this.targetVersion = targetVersion;
        this.previousVersion = targetVersion.previousVersion();
    }

    @Override
    public Version getTargetVersion() {
        return targetVersion;
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

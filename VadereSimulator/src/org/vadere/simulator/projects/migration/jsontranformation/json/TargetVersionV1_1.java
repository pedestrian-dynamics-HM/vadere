package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;

@MigrationTransformation(targetVersionLabel = "1.1")
public class TargetVersionV1_1 extends SimpleJsonTransformation {

    public TargetVersionV1_1() {
        super(Version.V1_1);
    }

    @Override
    protected void initDefaultHooks() {
        addPostHookLast(this::removeCarAttributes);
        addPostHookLast(this::addCommitHashWarningIfMissing);
        addPostHookLast(this::sort);
    }

    @Override
    public JsonNode applyTransformation(JsonNode node) throws MigrationException {
        return super.applyTransformation(node);
    }

    public JsonNode removeCarAttributes(JsonNode scenarioFile) throws MigrationException {
        JsonNode attributesCar = pathMustExist(scenarioFile, "scenario/topography/attributesCar");
        if (!attributesCar.isNull()){
            ObjectNode topography = (ObjectNode) pathMustExist(scenarioFile, "scenario/topography");
            topography.replace("attributesCar", NullNode.getInstance());
        }
        return scenarioFile;
    }

}

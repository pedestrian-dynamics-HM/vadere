package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;

@MigrationTransformation(targetVersionLabel = "0.10")
public class JsonTransformV9ToV010 extends SimpleJsonTransformation {

    public JsonTransformV9ToV010() {
        super(Version.V0_10);
    }

    @Override
    protected void initDefaultHooks() {
        addPostHookLast(this::addUseSalientBehaviorToAttributesSimulation);
        addPostHookLast(this::addAttributesToAttributesAgent);
        addPostHookLast(this::addCommitHashWarningIfMissing);
        addPostHookLast(this::sort);
    }

    @Override
    public JsonNode applyTransformation(JsonNode node) throws MigrationException {
        return super.applyTransformation(node);
    }

    public JsonNode addUseSalientBehaviorToAttributesSimulation(JsonNode scenarioFile) throws MigrationException {
        JsonNode attributesSimulation = pathMustExist(scenarioFile, "scenario/attributesSimulation");

        addBooleanField(attributesSimulation, "useSalientBehavior", false);

        return scenarioFile;
    }

    public JsonNode addAttributesToAttributesAgent(JsonNode scenarioFile) throws MigrationException {
        JsonNode attributesPedestrian = pathMustExist(scenarioFile, "scenario/topography/attributesPedestrian");

        addIntegerField(attributesPedestrian, "footStepsToStore", 4);
        addDoubleField(attributesPedestrian, "searchRadius", 1.0);
        addToObjectNode(attributesPedestrian, "angleCalculationType", "USE_CENTER");
        addDoubleField(attributesPedestrian, "targetOrientationAngleThreshold", 45.0);

        return scenarioFile;
    }

}

package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.AbstractJsonTransformation;

import java.util.Arrays;

// @MigrationTransformation not needed
public class JsonTransformation_Default extends AbstractJsonTransformation {


    private Version targetVersion;

    /**
     * Default Transformation is used if the only change is the version number. Thus a specific
     * classes such as VZtoVY is not needed. This Transformation does not need a
     * @MigrationTransformation annotation because it is added to the the
     * {@link org.vadere.simulator.projects.migration.jsontranformation.JsonTransformationBaseFactory}
     * directly thus code generation in the Factory class is not needed.
     */
    public JsonTransformation_Default() {
        super();
    }

    @Override
    public Version getTargetVersion() {
        return targetVersion;
    }

    @Override
    public JsonNode applyPreHooks(JsonNode node) throws MigrationException {
        // Target version not known prior to this call because this is the default transformation
        // which is used if no Version information is present a priori.
        setTargetVersion(node, true); // version in JsonNode not yet changed

        return super.applyPreHooks(node);
    }

    /**
     * Increase the version of the scenario file by 1 or leave it as is.
     */
    @Override
    public JsonNode applyTransformation(JsonNode node) throws MigrationException {
        // Target version not known prior to this call because this is the default transformation
        // which is used if no Version information is present a priori.
        setTargetVersion(node, true); // version in JsonNode not yet changed

        JsonNode ret = setVersionFromTo(node, targetVersion.previousVersion(), targetVersion);
        return ret;
    }

    @Override
    public JsonNode applyPostHooks(JsonNode node) throws MigrationException {
        // Target version not known prior to this call because this is the default transformation
        // which is used if no Version information is present a priori.
        setTargetVersion(node, false); // version in JsonNode already changed
        return super.applyPostHooks(node);
    }


    private void setTargetVersion(JsonNode node, boolean beforeTransformation) throws MigrationException{
        String currentVersionLabel = pathMustExist(node, "release").asText();
        Version currentVersion = Version.fromString(currentVersionLabel);
        if (currentVersion == null){
            throw new MigrationException(
                    String.format("Version string: %s in scenario does not match any valid version labels: %s",
                            currentVersionLabel, Arrays.toString(Version.stringValues())));
        }
        // if currentVersion == latest then the nextVersion == currentVersion
        if (beforeTransformation){
            this.targetVersion = currentVersion.nextVersion();
        } else {
            this.targetVersion = currentVersion;
        }
    }



    @Override
    protected void initDefaultHooks() {
        addPostHookLast(this::sort);
    }

}

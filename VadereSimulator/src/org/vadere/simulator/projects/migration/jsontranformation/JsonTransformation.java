package org.vadere.simulator.projects.migration.jsontranformation;

import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.simulator.projects.migration.MigrationException;

public interface JsonTransformation {

    JsonNode applyTransformation(JsonNode node) throws MigrationException;
    JsonNode applyPreHooks(JsonNode node) throws MigrationException;
    JsonNode applyPostHooks(JsonNode node) throws MigrationException;

    default JsonNode applyAll(JsonNode node) throws MigrationException{
        node = applyPreHooks(node);
        node = applyTransformation(node);
        node = applyPostHooks(node);
        return node;
    }

    void addPreHookFirst(JsonTransformationHook hook);
    void addPreHookLast(JsonTransformationHook hook);

    void addPostHookFirst(JsonTransformationHook hook);
    void addPostHookLast(JsonTransformationHook hook);
}

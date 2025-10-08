package org.vadere.state.attributes.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.util.StateJsonConverter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * The model with hash function usage cannot be used together with the psychology layer, if
 * a feature of the psychology layer affects the movement pattern
 * @author Kevin Becker, Sophia Wagner
 */
@ModelAttributeClass
public class AttributesDSM extends Attributes {

    private String trajectoryFile = "./temp/postvis.traj";
    private int bufferedLines = 1000;
    /**
     * This list should only be used if fallbackMainModel is null.
     * Otherwise, the submodels list of fallbackMainModel will be used.
     */
    private List<String> submodels = new LinkedList<>();
    /**
     * the main model if the trajectory file is not found
     */
    private String fallbackMainModel = null;
    private JsonNode attributesFallbackModel;

    public String getTrajectoryFile() {
        return trajectoryFile;
    }

    public List<String> getSubmodels() {
        return new ArrayList<>(submodels);
    }

    public int getBufferedLines() {
        return bufferedLines;
    }

    public String getFallbackMainModel() {
        return fallbackMainModel;
    }

    public List<Attributes> getAttributesFallbackModel() {
        try {
            return StateJsonConverter.deserializeAttributesListFromNode(attributesFallbackModel);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setTrajectoryFile(String trajectoryFile) {
        this.trajectoryFile = trajectoryFile;
    }
}
package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

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
    private List<Attributes> attributesFallbackModel = new ArrayList<>();

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
        return attributesFallbackModel;
    }

    public void setTrajectoryFile(String trajectoryFile) {
        this.trajectoryFile = trajectoryFile;
    }
}
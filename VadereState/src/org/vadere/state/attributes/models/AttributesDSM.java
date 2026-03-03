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
 * @author Kevin Becker, Sophia Wagner
 */
@ModelAttributeClass
public class AttributesDSM extends Attributes {

    /**
     * Can be either a .traj file, from which the steps of the pedestrians can be extracted,
     * or a folder, in which either a .traj file with the corresponding hash is stored or if not,
     * will be created using the fallbackMainModel and stored in the folder
     */
    private String trajectoryFileOrFolder = null;
    private int bufferedLines = 1000;
    /**
     * Option to delete the postvis.traj file from the scenario output folder. This is useful
     * for parameter studies, since the postvis.traj file take up a lot of space.
     */
    private boolean deletePostvisFile = true;
    /**
     * This list should only be used if fallbackMainModel is null.
     * Otherwise, the submodels list of fallbackMainModel will be used.
     */
    private List<String> submodels = new LinkedList<>();

    /**
     * the main model if the trajectory file is not found
     */
    private String fallbackMainModel = null;
    /**
     * the attributes of the fallback main model if the trajectory file is not found, including all types of attributes
     * that affect movement patterns of the agents,
     * e.g. AttributesOSM, AttributesPotentialCompactSoftshell and AttributesFloorField for the OSM.
     * Same format as attributesModel.
     */
    private JsonNode attributesFallbackModel = null;

    public String getTrajectoryFileOrFolder() {
        return trajectoryFileOrFolder;
    }

    public boolean isDeletePostvisFile() { return deletePostvisFile; }

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

    public void setTrajectoryFileOrFolder(String trajectoryFileOrFolder) {
        this.trajectoryFileOrFolder = trajectoryFileOrFolder;
    }
}
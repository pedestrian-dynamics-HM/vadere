package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Kevin Becker
 */
@ModelAttributeClass
public class AttributesDSM extends Attributes {
    private String trajectoryFile = "./temp/postvis.traj";
    private List<String> submodels = new LinkedList<>();
    private int bufferedLines = 1000;

    public String getTrajectoryFile() {
        return trajectoryFile;
    }

    public List<String> getSubmodels() {
        return new ArrayList<>(submodels);
    }

    public int getBufferedLines() {
        return bufferedLines;
    }
}
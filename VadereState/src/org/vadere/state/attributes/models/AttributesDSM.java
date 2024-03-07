package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Kevin Becker
 */
@ModelAttributeClass
public class AttributesDSM extends Attributes {
    private String trajotoryFile = "./temp/postvis.traj";
    private List<String> submodels = new LinkedList<>();
    private int bufferedLines = 1000;

    public String getTrajotoryFile() {
        return trajotoryFile;
    }

    public List<String> getSubmodels() {
        return submodels;
    }

    public int getBufferedLines() {
        return bufferedLines;
    }
}
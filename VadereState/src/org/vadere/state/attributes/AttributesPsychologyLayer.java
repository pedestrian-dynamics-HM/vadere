package org.vadere.state.attributes;

import org.vadere.state.attributes.models.psychology.cognition.AttributesSimpleCognitionModel;
import org.vadere.state.attributes.models.psychology.perception.AttributesSimplePerceptionModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class beautifies the JSON content by mapping
 * the words "perception" and "cognition" to existing class names.
 */
public class AttributesPsychologyLayer {

    // Constants
    // Watch out: Make sure these classes exist!
    public static final String DEFAULT_PERCEPTION_MODEL = "SimplePerceptionModel";
    public static final String DEFAULT_COGNITION_MODEL = "SimpleCognitionModel";


    // Variables
    // Both should reference to concrete "IPerception" and "ICognition"
    // implementations! We do not reference them here to avoid cyclic
    // dependencies between state and controller packages
    private String perception;
    private String cognition;

    public List<Attributes> attributesModel;

    // Constructors
    public AttributesPsychologyLayer() {
        this(DEFAULT_PERCEPTION_MODEL, DEFAULT_COGNITION_MODEL, new ArrayList<>());
        this.attributesModel.add(0, new AttributesSimplePerceptionModel());
        this.attributesModel.add(1, new AttributesSimpleCognitionModel());
    }

    public AttributesPsychologyLayer(String perception, String cognition, List<Attributes> attributesModel ) {
        this.perception = perception;
        this.cognition = cognition;
        this.attributesModel = attributesModel;
    }

    // Getter
    public String getPerception() {
        return perception;
    }

    public String getCognition() {
        return cognition;
    }

    // Setter
    public void setPerception(String perception) {
        this.perception = perception;
    }

    public void setCognition(String cognition) {
        this.cognition = cognition;
    }

    // Overridden Methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributesPsychologyLayer that = (AttributesPsychologyLayer) o;
        return Objects.equals(perception, that.perception) &&
                Objects.equals(cognition, that.cognition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perception, cognition);
    }


    public List<Attributes> getAttributesModel() {
        return attributesModel;
    }

    public void setAttributesModel(List<Attributes> attributesModel) {
        this.attributesModel = attributesModel;
    }



}

package org.vadere.state.attributes;

import java.util.Objects;

/**
 * This wrapper encapsulates psychology-related simulation parameters.
 */
public class AttributesPsychology {

    // Constants
    public static final String DEFAULT_PERCEPTION_MODEL = "SimplePerceptionModel";
    public static final String DEFAULT_COGNITION_MODEL = "SimpleCognitionModel";

    // Variables
    // Both should reference to concrete "IPerception" and "ICognition"
    // implementations! We do not reference them here to avoid cyclic
    // dependencies between state and controller packages
    private String perception;
    private String cognition;

    // Constructors
    public AttributesPsychology() {
        this(DEFAULT_PERCEPTION_MODEL, DEFAULT_COGNITION_MODEL);
    }

    public AttributesPsychology(String perception, String cognition) {
        this.perception = perception;
        this.cognition = cognition;
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
        AttributesPsychology that = (AttributesPsychology) o;
        return Objects.equals(perception, that.perception) &&
                Objects.equals(cognition, that.cognition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perception, cognition);
    }

}

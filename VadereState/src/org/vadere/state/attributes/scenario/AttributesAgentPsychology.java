package org.vadere.state.attributes.scenario;

import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.Stimulus;

public class AttributesAgentPsychology {

    // Member Variables
    private Stimulus mostImportantStimulus;
    private SelfCategory selfCategory;

    // Constructors
    public AttributesAgentPsychology() {
        this(null, SelfCategory.TARGET_ORIENTED);
    }

    public AttributesAgentPsychology(Stimulus mostImportantStimulus, SelfCategory selfCategory) {
        this.mostImportantStimulus = mostImportantStimulus;
        this.selfCategory = selfCategory;
    }

    // Getter
    public Stimulus getMostImportantStimulus() { return mostImportantStimulus; }
    public SelfCategory getSelfCategory() { return selfCategory; }

    // Setter
    public void setMostImportantStimulus(Stimulus mostImportantStimulus) {
        this.mostImportantStimulus = mostImportantStimulus;
    }

    public void setSelfCategory(SelfCategory selfCategory) {
        this.selfCategory = selfCategory;
    }

}

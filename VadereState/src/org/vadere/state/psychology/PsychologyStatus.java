package org.vadere.state.psychology;

import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.Stimulus;

public class PsychologyStatus {

    // Member Variables
    private Stimulus mostImportantStimulus;
    private SelfCategory selfCategory;

    // Constructors
    public PsychologyStatus() {
        this(null, SelfCategory.TARGET_ORIENTED);
    }

    public PsychologyStatus(Stimulus mostImportantStimulus, SelfCategory selfCategory) {
        this.mostImportantStimulus = mostImportantStimulus;
        this.selfCategory = selfCategory;
    }

    public PsychologyStatus(PsychologyStatus other) {
        this.mostImportantStimulus = other.getMostImportantStimulus() != null ? other.getMostImportantStimulus().clone() : null;
        this.selfCategory = other.getSelfCategory();
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

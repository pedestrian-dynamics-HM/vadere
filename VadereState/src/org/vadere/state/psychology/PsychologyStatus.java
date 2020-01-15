package org.vadere.state.psychology;

import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.Stimulus;

public class PsychologyStatus {

    // Member Variables
    private Stimulus mostImportantStimulus;
    // TODO: Maybe, implement some kind of memory instead of only a perceived threat.
    private Stimulus perceivedThreat;
    private SelfCategory selfCategory;

    // Constructors
    public PsychologyStatus() {
        this(null, null, SelfCategory.TARGET_ORIENTED);
    }

    public PsychologyStatus(Stimulus mostImportantStimulus, Stimulus perceivedThreat, SelfCategory selfCategory) {
        this.mostImportantStimulus = mostImportantStimulus;
        this.perceivedThreat = perceivedThreat;
        this.selfCategory = selfCategory;
    }

    public PsychologyStatus(PsychologyStatus other) {
        this.mostImportantStimulus = other.getMostImportantStimulus() != null ? other.getMostImportantStimulus().clone() : null;
        this.perceivedThreat = other.getPerceivedThreat() != null ? other.getPerceivedThreat().clone() : null;
        this.selfCategory = other.getSelfCategory();
    }

    // Getter
    public Stimulus getMostImportantStimulus() { return mostImportantStimulus; }
    public Stimulus getPerceivedThreat() { return perceivedThreat; }
    public SelfCategory getSelfCategory() { return selfCategory; }

    // Setter
    public void setMostImportantStimulus(Stimulus mostImportantStimulus) {
        this.mostImportantStimulus = mostImportantStimulus;
    }

    public void setPerceivedThreat(Stimulus perceivedThreat) {
        this.perceivedThreat = perceivedThreat;
    }

    public void setSelfCategory(SelfCategory selfCategory) {
        this.selfCategory = selfCategory;
    }

}

package org.vadere.state.psychology;

import org.vadere.state.psychology.cognition.GroupMembership;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.Stimulus;

public class PsychologyStatus {

    // Member Variables
    private Stimulus mostImportantStimulus;
    private Stimulus perceivedThreat; // TODO: Maybe, implement some kind of memory instead of just a perceived threat.
    private SelfCategory selfCategory;
    private GroupMembership groupMembership;

    // Constructors
    public PsychologyStatus() {
        this(null, null, SelfCategory.TARGET_ORIENTED, GroupMembership.OUT_GROUP);
    }

    public PsychologyStatus(Stimulus mostImportantStimulus, Stimulus perceivedThreat, SelfCategory selfCategory, GroupMembership groupMembership) {
        this.mostImportantStimulus = mostImportantStimulus;
        this.perceivedThreat = perceivedThreat;
        this.selfCategory = selfCategory;
        this.groupMembership = groupMembership;
    }

    public PsychologyStatus(PsychologyStatus other) {
        this.mostImportantStimulus = other.getMostImportantStimulus() != null ? other.getMostImportantStimulus().clone() : null;
        this.perceivedThreat = other.getPerceivedThreat() != null ? other.getPerceivedThreat().clone() : null;
        this.selfCategory = other.getSelfCategory();
        this.groupMembership = other.getGroupMembership();
    }

    // Getter
    public Stimulus getMostImportantStimulus() { return mostImportantStimulus; }
    public Stimulus getPerceivedThreat() { return perceivedThreat; }
    public SelfCategory getSelfCategory() { return selfCategory; }
    public GroupMembership getGroupMembership() { return groupMembership; }

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

    public void setGroupMembership(GroupMembership groupMembership) {
        this.groupMembership = groupMembership;
    }

}

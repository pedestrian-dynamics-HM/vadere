package org.vadere.state.psychology;

import org.vadere.state.psychology.cognition.GroupMembership;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.information.InformationState;
import org.vadere.state.psychology.information.KnowledgeBase;
import org.vadere.state.psychology.perception.ThreatMemory;
import org.vadere.state.psychology.perception.types.ElapsedTime;
import org.vadere.state.psychology.perception.types.Stimulus;

import java.util.LinkedList;

public class PsychologyStatus {

    // Member Variables
    private Stimulus mostImportantStimulus;
    private ThreatMemory threatMemory;
    private SelfCategory selfCategory;
    private GroupMembership groupMembership;
    private KnowledgeBase knowledgeBase;
    private LinkedList<Stimulus> perceivedStimuli;
    private LinkedList<Stimulus> nextPerceivedStimuli;




    // Constructors
    public PsychologyStatus() {
        this(null, new ThreatMemory(), SelfCategory.TARGET_ORIENTED, GroupMembership.OUT_GROUP, new KnowledgeBase());
    }

    public PsychologyStatus(Stimulus mostImportantStimulus, ThreatMemory threatMemory, SelfCategory selfCategory, GroupMembership groupMembership, KnowledgeBase knowledgeBase) {
        this.mostImportantStimulus = mostImportantStimulus;
        this.threatMemory = threatMemory;
        this.selfCategory = selfCategory;
        this.groupMembership = groupMembership;
        this.knowledgeBase = knowledgeBase;
        this.perceivedStimuli = new LinkedList<>();
        this.nextPerceivedStimuli = new LinkedList<>();
    }

    public PsychologyStatus(PsychologyStatus other) {
        this.mostImportantStimulus = other.getMostImportantStimulus() != null ? other.getMostImportantStimulus().clone() : null;
        this.threatMemory = other.getThreatMemory() != null ? other.getThreatMemory().clone() : null;
        this.selfCategory = other.getSelfCategory();
        this.groupMembership = other.getGroupMembership();
        this.knowledgeBase = other.getKnowledgeBase();
        this.perceivedStimuli = other.getPerceivedStimuli();
        this.nextPerceivedStimuli = other.getNextPerceivedStimuli();
    }

    // Getter
    public Stimulus getMostImportantStimulus() { return mostImportantStimulus; }
    public ThreatMemory getThreatMemory() { return threatMemory; }
    public SelfCategory getSelfCategory() { return selfCategory; }
    public GroupMembership getGroupMembership() { return groupMembership; }
    public KnowledgeBase getKnowledgeBase() { return knowledgeBase; }
    public LinkedList<Stimulus> getPerceivedStimuli() { return perceivedStimuli; }
    public LinkedList<Stimulus> getNextPerceivedStimuli() { return nextPerceivedStimuli; }


    // Setter
    public void setMostImportantStimulus(Stimulus mostImportantStimulus) {
        this.mostImportantStimulus = mostImportantStimulus;
        if (!(mostImportantStimulus instanceof ElapsedTime)){
            this.getKnowledgeBase().setInformationState(InformationState.INFORMATION_STIMULUS);
        }
    }

    public void setPerceivedStimuli(LinkedList<Stimulus> perceivedStimuli){
        this.perceivedStimuli = perceivedStimuli;
    }

    public void setThreatMemory(ThreatMemory threatMemory) { this.threatMemory = threatMemory; }

    public void setSelfCategory(SelfCategory selfCategory) {
        this.selfCategory = selfCategory;
    }

    public void setGroupMembership(GroupMembership groupMembership) {
        this.groupMembership = groupMembership;
    }

    public void setNextPerceivedStimuli(final LinkedList<Stimulus> nextPerceivedStimuli) { this.nextPerceivedStimuli = nextPerceivedStimuli; }


}

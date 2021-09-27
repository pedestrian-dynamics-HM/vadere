package org.vadere.state.psychology.perception.json;


public class ReactionProbability {

    // Member Variables
    private int stimulusId;
    private double reactionProbability;

    public ReactionProbability(){
        this.stimulusId = -1;
        this.reactionProbability = 1.0;
    }

    public ReactionProbability(double reactionProbability){
        this.stimulusId = -1;
        this.reactionProbability = reactionProbability;
    }

    public ReactionProbability(int stimulusId,double reactionProbability){
        this.stimulusId = stimulusId;
        this.reactionProbability = reactionProbability;
    }


    public int getStimulusId() {
        return stimulusId;
    }
    public double getReactionProbability() {
        return reactionProbability;
    }

    public void setStimulusId(final int stimulusId) {
        this.stimulusId = stimulusId;
    }

    public void setReactionProbability(final double reactionProbability) {
        this.reactionProbability = reactionProbability;
    }

    @Override
    public String toString(){
        String string = String.format("StimulusId: %d\n", this.stimulusId);
        string += String.format("ReactionProbability: %f\n", this.reactionProbability);
        return string;
    }
}

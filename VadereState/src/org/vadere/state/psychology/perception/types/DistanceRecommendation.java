package org.vadere.state.psychology.perception.types;

import org.apache.commons.math3.util.Precision;

/**
 * Class can signal agents to wait - for instance at a red traffic light.
 */
public class DistanceRecommendation extends Stimulus {

    private double socialDistance = 1.5; // must be between 1.25 and 2.0
    private double cloggingTimeAllowedInSecs = 5.0; // time in seconds after agents fall back to default behavior in case of clogging

    // Default constructor required for JSON de-/serialization.
    public DistanceRecommendation() { super(); }

    public DistanceRecommendation(double time) {
        super(time);
    }

    public DistanceRecommendation(double time, double socialDistance, double cloggingTimeAllowedInSecs) {
        super(time);
        this.socialDistance = socialDistance;
        this.cloggingTimeAllowedInSecs = cloggingTimeAllowedInSecs;
    }

    public DistanceRecommendation(DistanceRecommendation other){
        this.time = other.time;
        this.socialDistance= other.socialDistance;
        this.cloggingTimeAllowedInSecs = other.cloggingTimeAllowedInSecs;
    }

    // Getters
    public double getSocialDistance() { return socialDistance; }
    public double getCloggingTimeAllowedInSecs() { return cloggingTimeAllowedInSecs; }


    // Methods
    @Override
    public DistanceRecommendation clone() {
        return new DistanceRecommendation(this);
    }

    @Override
    public boolean equals(Object that){
        if(this == that) return true;
        if(!(that instanceof DistanceRecommendation)) return false;
        DistanceRecommendation thatStimulus = (DistanceRecommendation) that;
        return true;
    }

}

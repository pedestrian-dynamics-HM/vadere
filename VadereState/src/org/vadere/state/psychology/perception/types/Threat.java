package org.vadere.state.psychology.perception.types;

import org.apache.commons.math3.util.Precision;

/**
 * Class signals agents a threat. For instance that something exploded.
 *
 * This stimulus holds one additional information: a target id
 * which represents the origin of the threat.
 */
public class Threat extends Stimulus {

    // Member Variables
    private int originAsTargetId = -1;
    private double loudness = 1;
    private double radius = 5;

    // Constructors
    // Default constructor required for JSON de-/serialization.
    public Threat() { super(); }

    public Threat(double time) {
        super(time);
    }

    public Threat(double time, double probability) {
        super(time, probability);
    }

    public Threat(double time, double probability, int id) {
        super(time, probability, id);
    }


    public Threat(double time, int originAsTargetId) {
        super(time);

        this.originAsTargetId = originAsTargetId;
    }



    public Threat(Threat other) {
        super(other);

        this.originAsTargetId = other.getOriginAsTargetId();
        this.loudness = other.getLoudness();
        this.radius = other.getRadius();
    }

    // Getter
    public int getOriginAsTargetId() { return originAsTargetId; }
    public double getLoudness() { return loudness; }
    public double getRadius() { return radius; }

    // Setter
    public void setOriginAsTargetId(int originAsTargetId) { this.originAsTargetId = originAsTargetId; }
    public void setLoudness(double loudness) { this.loudness = loudness; }
    public void setRadius(double radius) { this.radius = radius; }

    @Override
    public Threat clone() {
        return new Threat(this);
    }

    @Override
    public boolean equals(Object that){
        if(this == that) return true;
        if(!(that instanceof Threat)) return false;
        Threat threat = (Threat) that;
        boolean isProb = Precision.equals(this.perceptionProbability, threat.getPerceptionProbability(), Double.MIN_VALUE);
        boolean loud = Precision.equals(this.loudness, threat.getLoudness(), Double.MIN_VALUE);
        boolean radius = Precision.equals(this.radius, threat.getRadius(), Double.MIN_VALUE);
        return isProb && loud && radius;
    }
}

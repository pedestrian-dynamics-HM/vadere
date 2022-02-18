package org.vadere.state.health;

/**
 * Defines a pedestrian's infection status by means of a probability of infection.
 */
public class ThresholdResponseModelInfectionStatus implements DoseResponseModelInfectionStatus {

    private double probabilityOfInfection;

    /*
     * default for pedestrian's probability of infection, when pedestrian has not been exposed yet
     */
    private static final double DEF_PROBABILITY_OF_INFECTION = 0;


    // Constructors
    public ThresholdResponseModelInfectionStatus(double probabilityOfInfection) {
        this.probabilityOfInfection = probabilityOfInfection;
    }

    public ThresholdResponseModelInfectionStatus(ThresholdResponseModelInfectionStatus other) {
        this.probabilityOfInfection = other.getProbabilityOfInfection();
    }

    public ThresholdResponseModelInfectionStatus() {
        this(DEF_PROBABILITY_OF_INFECTION);
    }

    // Getter
    @Override
    public double getProbabilityOfInfection() {
        return probabilityOfInfection;
    }

    // Setter
    @Override
    public void setProbabilityOfInfection(double probabilityOfInfection) {
        this.probabilityOfInfection = probabilityOfInfection;
    }
}

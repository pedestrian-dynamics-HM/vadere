package org.vadere.state.health;

public abstract class DoseResponseModelInfectionStatus {

    /*
     * Probability of infection; depending on the dose response model, it ranges in [0, 1] or {0, 1}.
     */
    private double probabilityOfInfection;

    private static final double MIN_PROBABILITY_INFECTION = 0;
    private static final double MAX_PROBABILITY_INFECTION = 1;

    DoseResponseModelInfectionStatus() {
        this.probabilityOfInfection = MIN_PROBABILITY_INFECTION;
    }

    // Getter
    public double getProbabilityOfInfection() {
        return probabilityOfInfection;
    }

    // Setter
    public void setProbabilityOfInfection(double probabilityOfInfection) {
        if(probabilityOfInfection < MIN_PROBABILITY_INFECTION || probabilityOfInfection > MAX_PROBABILITY_INFECTION) {
            throw new IllegalArgumentException();
        } else {
            this.probabilityOfInfection = probabilityOfInfection;
        }
    }

    public void setProbabilityOfInfectionToMax() {
        this.probabilityOfInfection = MAX_PROBABILITY_INFECTION;
    }
}

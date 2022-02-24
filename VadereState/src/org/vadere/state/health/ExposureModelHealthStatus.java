package org.vadere.state.health;

public abstract class ExposureModelHealthStatus {

    boolean infectious;
    double degreeOfExposure;

    ExposureModelHealthStatus() {
        this(false, 0);
    }

    ExposureModelHealthStatus(boolean infectious, double degreeOfExposure) {
        this.infectious = infectious;
        this.degreeOfExposure = degreeOfExposure;
    }

    // Getter
    public boolean isInfectious() {
        return infectious;
    }

    public double getDegreeOfExposure() {
        return degreeOfExposure;
    }

    // Setter
    public void setInfectious(boolean infectious) {
        this.infectious = infectious;
    }

    public void setDegreeOfExposure(double degreeOfExposure) {
        this.degreeOfExposure = degreeOfExposure;
    }

    // Methods
    public void incrementDegreeOfExposure(double deltaDegreeOfExposure) {
        this.degreeOfExposure += deltaDegreeOfExposure;
    }
}

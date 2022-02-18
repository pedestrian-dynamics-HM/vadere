package org.vadere.state.health;

public interface ExposureModelHealthStatus {
    // Getter
    boolean isInfectious();

    double getDegreeOfExposure();

    // Setter
    void setInfectious(boolean infectious);

    void setDegreeOfExposure(double degreeOfExposure);
}

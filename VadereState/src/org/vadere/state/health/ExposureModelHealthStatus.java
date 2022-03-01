package org.vadere.state.health;

/**
 * ExposureModelHealthStatus is the abstract base class for all types of exposure
 * a <code>Pedestrian</code> can adopt.
 * <p>
 *     It describes the degree to which a <code>Pedestrian</code> is exposed to
 *     infectious pathogens, or in a more abstract sense, to other (infectious)
 *     agents. The degree of exposure is defined by the underlying
 *     <code>AbstractExposureModel</code>.
 * </p>
 */
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

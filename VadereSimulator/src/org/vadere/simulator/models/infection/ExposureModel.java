package org.vadere.simulator.models.infection;

import org.vadere.simulator.models.Model;
import org.vadere.state.scenario.Pedestrian;

public interface ExposureModel extends Model {


    /**
     * This method updates the degree of exposure of a pedestrian.
     */
    void updatePedestrianDegreeOfExposure(final Pedestrian pedestrian, double degreeOfExposure);
}

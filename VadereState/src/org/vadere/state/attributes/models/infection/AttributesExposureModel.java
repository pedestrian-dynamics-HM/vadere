package org.vadere.state.attributes.models.infection;

import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * AttributesExposureModel contains user-defined properties describing the
 * <code>AbstractExposureModel</code>.
 */
public abstract class AttributesExposureModel extends Attributes {

    private ArrayList<AttributesExposureModelSourceParameters> exposureModelSourceParameters;

    /**
     * Contains the Ids of pedestrians that are directly set into the topography (and not spawned by  sources).
     * Any agent contained in the list is infectious. All others (not spawned by sources) are not infectious.
     */
    private ArrayList<Integer> infectiousPedestrianIdsNoSource;

    public AttributesExposureModel() {
        this.exposureModelSourceParameters = new ArrayList<>(Arrays.asList(new AttributesExposureModelSourceParameters()));
        this.infectiousPedestrianIdsNoSource = new ArrayList<>();
    }

    public ArrayList<AttributesExposureModelSourceParameters> getExposureModelSourceParameters() {
        return exposureModelSourceParameters;
    }

    public ArrayList<Integer> getInfectiousPedestrianIdsNoSource() {
        return infectiousPedestrianIdsNoSource;
    }

    public void addInfectiousPedestrianIdsNoSource(int pedestrianId) {
        infectiousPedestrianIdsNoSource.add(pedestrianId);
    }
}

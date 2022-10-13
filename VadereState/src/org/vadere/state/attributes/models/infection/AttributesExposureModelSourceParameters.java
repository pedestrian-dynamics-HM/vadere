package org.vadere.state.attributes.models.infection;

import org.vadere.state.attributes.Attributes;

/**
 * Attributes required by an exposure model to define which source (defined by {@link #sourceId}) spawns
 * {@link #infectious} agents.
 */
public class AttributesExposureModelSourceParameters extends Attributes {

    /**
     * Default value -1 refers to any source that has not referenced explicitly by another sourceId.
     */
    private int sourceId;

    /**
     * Describes whether agents from this source are infectious or not.
     */
    private boolean infectious;



    public AttributesExposureModelSourceParameters(int sourceId, boolean infectious) {
        this.sourceId = sourceId;
        this.infectious = infectious;
    }

    public AttributesExposureModelSourceParameters() {
        this(Attributes.ID_NOT_SET, false);
    }

    public boolean isInfectious() {
        return infectious;
    }

    public int getSourceId() {
        return sourceId;
    }
}

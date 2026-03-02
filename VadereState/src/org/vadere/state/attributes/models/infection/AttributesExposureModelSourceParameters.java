package org.vadere.state.attributes.models.infection;

import org.vadere.state.attributes.Attributes;

import java.util.LinkedList;
import java.util.List;

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

    /**
     * Describes the spawnIds of the Agents spawned by this source, who are infectious.
     */
    private List<Integer> infectiousSpawnIds = new LinkedList<>();


    public AttributesExposureModelSourceParameters(int sourceId, boolean infectious, List<Integer> infectiousSpawnIds) {
        this.sourceId = sourceId;
        this.infectious = infectious;
        this.infectiousSpawnIds = infectiousSpawnIds;
    }

    public AttributesExposureModelSourceParameters() {
        this(Attributes.ID_NOT_SET, false, List.of(0));
    }

    public boolean isInfectious() {
        return infectious;
    }

    public int getSourceId() {
        return sourceId;
    }

    public List<Integer> getInfectiousSpawnIds() {
        return infectiousSpawnIds;
    }
}

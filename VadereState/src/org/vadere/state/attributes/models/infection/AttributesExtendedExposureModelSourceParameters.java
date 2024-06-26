package org.vadere.state.attributes.models.infection;

import org.vadere.state.attributes.Attributes;

import java.util.LinkedList;
import java.util.List;

public class AttributesExtendedExposureModelSourceParameters extends AttributesExposureModelSourceParameters {

    /**
     * Describes the spawnIds of the Agents spawned by this source, who are infectious.
     */
    private List<Integer> infectiousSpawnIds = new LinkedList<>();
    /**
     * Describes whether agents from this source are talking and therefore emit more pathogens or not.
     */
    private boolean talking;

    /**
     * Describes whether agents from this source are coughing and therefore emit more pathogens or not.
     */
    private boolean coughing;

    /**
     * Describes whether agents from this source are sneezing and therefore emit more pathogens or not.
     */
    private boolean sneezing;

    /**
     * Describes whether agents from this source are coughing and therefore emit more pathogens or not.
     */
    private int coughingEveryNthBreath;

    /**
     * Describes whether agents from this source are sneezing and therefore emit more pathogens or not.
     */
    private int sneezingEveryNthBreath;

    public AttributesExtendedExposureModelSourceParameters(boolean talking, boolean coughing, boolean sneezing, int coughingEveryNthBreath, int sneezingEveryNthBreath, List<Integer> infectiousSpawnIds) {
        this.talking = talking;
        this.coughing = coughing;
        this.sneezing = sneezing;
        this.coughingEveryNthBreath = coughingEveryNthBreath;
        this.sneezingEveryNthBreath = sneezingEveryNthBreath;
        this.infectiousSpawnIds = infectiousSpawnIds;
    }

    public AttributesExtendedExposureModelSourceParameters() {
        this(false, false, false, -1, -1, new LinkedList<>());
    }

    public boolean isTalking() {
        return talking;
    }

    public boolean isCoughing() {
        return coughing;
    }

    public boolean isSneezing() {
        return sneezing;
    }

    public int getCoughingEveryNthBreath() { return coughingEveryNthBreath;  }

    public int getSneezingEveryNthBreath() { return sneezingEveryNthBreath;  }

    public List<Integer> getInfectiousSpawnIds() {
        return infectiousSpawnIds;
    }
}

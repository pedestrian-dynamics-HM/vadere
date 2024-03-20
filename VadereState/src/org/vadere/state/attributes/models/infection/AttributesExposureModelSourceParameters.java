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



    public AttributesExposureModelSourceParameters(int sourceId, boolean infectious, boolean talking, boolean coughing, boolean sneezing, int coughingEveryNthBreath, int sneezingEveryNthBreath) {
        this.sourceId = sourceId;
        this.infectious = infectious;
        this.talking = talking;
        this.coughing = coughing;
        this.sneezing = sneezing;
        this.coughingEveryNthBreath = coughingEveryNthBreath;
        this.sneezingEveryNthBreath = sneezingEveryNthBreath;
    }

    public AttributesExposureModelSourceParameters() {
        this(Attributes.ID_NOT_SET, false, false, false, false, -1, -1);
    }

    public boolean isInfectious() {
        return infectious;
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

    public int getSourceId() {
        return sourceId;
    }
}

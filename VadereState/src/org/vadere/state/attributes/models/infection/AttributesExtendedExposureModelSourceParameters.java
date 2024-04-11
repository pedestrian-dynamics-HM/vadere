package org.vadere.state.attributes.models.infection;

import org.vadere.state.attributes.Attributes;

public class AttributesExtendedExposureModelSourceParameters extends AttributesExposureModelSourceParameters {

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

    public AttributesExtendedExposureModelSourceParameters(boolean talking, boolean coughing, boolean sneezing, int coughingEveryNthBreath, int sneezingEveryNthBreath) {
        this.talking = talking;
        this.coughing = coughing;
        this.sneezing = sneezing;
        this.coughingEveryNthBreath = coughingEveryNthBreath;
        this.sneezingEveryNthBreath = sneezingEveryNthBreath;
    }

    public AttributesExtendedExposureModelSourceParameters() {
        this(false, false, false, -1, -1);
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
}

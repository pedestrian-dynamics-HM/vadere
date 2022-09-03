package org.vadere.state.attributes.scenario;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.util.reflection.VadereAttribute;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AttributesWaitingArea extends AttributesVisualElement{
    @VadereAttribute
    private AttributesDistribution distribution;

    public AttributesDistribution getDistribution() {
        return distribution;
    }

    public AttributesWaitingArea setDistribution(AttributesDistribution distribution) {
        this.distribution = distribution;
        return this;
    }
}

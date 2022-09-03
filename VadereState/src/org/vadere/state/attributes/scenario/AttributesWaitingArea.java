package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.distributions.AttributesDistribution;

public class AttributesWaitingArea extends AttributesVisualElement{
    private AttributesDistribution distribution;

    public AttributesDistribution getDistribution() {
        return distribution;
    }

    public AttributesWaitingArea setDistribution(AttributesDistribution distribution) {
        this.distribution = distribution;
        return this;
    }
}

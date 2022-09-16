package org.vadere.state.attributes;

import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.util.reflection.VadereAttribute;

public class AttributesWaiter extends AttributesEnabled {
    @VadereAttribute
    private AttributesDistribution distribution;

    public AttributesWaiter(){
        super();
    }

    public AttributesWaiter(boolean enabled){
        super(enabled);
    }

    public AttributesDistribution getDistribution() {
        return distribution;
    }

    public AttributesWaiter setDistribution(AttributesDistribution distribution) {
        this.distribution = distribution;
        return this;
    }
}

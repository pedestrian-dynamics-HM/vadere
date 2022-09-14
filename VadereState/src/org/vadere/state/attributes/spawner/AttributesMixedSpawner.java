package org.vadere.state.attributes.spawner;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.attributes.distributions.AttributesMixedDistribution;
import org.vadere.state.util.Views;

public class AttributesMixedSpawner extends AttributesSpawner {
    @JsonView(Views.CacheViewExclude.class)
    protected AttributesMixedDistribution distribution = new AttributesMixedDistribution();
    public AttributesMixedSpawner(){
        super(new AttributesMixedDistribution());
    }

    @Override
    public AttributesDistribution getDistributionAttributes() {
        return distribution;
    }

    @Override
    public void setDistributionAttributes(AttributesDistribution distribution) {
        checkSealed();
        this.distribution = (AttributesMixedDistribution) distribution;
    }
}

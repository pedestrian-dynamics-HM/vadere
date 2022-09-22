package org.vadere.state.attributes.spawner;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.attributes.distributions.AttributesLinearInterpolationDistribution;
import org.vadere.state.util.Views;

public class AttributesLerpSpawner extends AttributesSpawner {
    public AttributesLerpSpawner(){
        super(new AttributesLinearInterpolationDistribution());
    }
    /**
     * This attribute controls the event times at which agents can be spawned.
     */
    @JsonView(Views.CacheViewExclude.class)
    protected AttributesLinearInterpolationDistribution distribution = new AttributesLinearInterpolationDistribution();
    @Override
    public AttributesDistribution getDistributionAttributes() {
        return distribution;
    }

    @Override
    public void setDistributionAttributes(AttributesDistribution distribution) {
        checkSealed();
        this.distribution = (AttributesLinearInterpolationDistribution) distribution;
    }

}

package org.vadere.state.attributes.spawner;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.attributes.distributions.AttributesConstantDistribution;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.util.Views;

public class AttributesRegularSpawner extends AttributesSpawner{

    @JsonView(Views.CacheViewExclude.class)
    protected AttributesDistribution distribution = null;
    public AttributesRegularSpawner(){
        super(new AttributesConstantDistribution());
    }

    @Override
    public AttributesDistribution getDistributionAttributes() {
        return distribution;
    }

    @Override
    public void setDistributionAttributes(AttributesDistribution distribution) {
        checkSealed();
        this.distribution = distribution;
    }

}

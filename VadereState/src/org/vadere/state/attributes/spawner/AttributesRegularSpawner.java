package org.vadere.state.attributes.spawner;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.attributes.distributions.AttributesConstantDistribution;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.util.Views;

public class AttributesRegularSpawner extends AttributesSpawner{
    /**
     * This attribute controls the event times at which agents can be spawned.
     */
    @JsonView(Views.CacheViewExclude.class)
    protected AttributesDistribution distribution = new AttributesConstantDistribution(1.0);
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

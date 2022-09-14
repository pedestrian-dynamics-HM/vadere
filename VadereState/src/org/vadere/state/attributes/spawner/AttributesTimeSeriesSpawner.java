package org.vadere.state.attributes.spawner;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.attributes.distributions.AttributesTimeSeriesDistribution;
import org.vadere.state.util.Views;

public class AttributesTimeSeriesSpawner extends AttributesSpawner {

    @JsonView(Views.CacheViewExclude.class)
    protected AttributesTimeSeriesDistribution distribution = new AttributesTimeSeriesDistribution();
    public AttributesTimeSeriesSpawner(){
        super(new AttributesTimeSeriesDistribution());
    }

    @Override
    public AttributesDistribution getDistributionAttributes() {
        return distribution;
    }

    @Override
    public void setDistributionAttributes(AttributesDistribution distribution) {
        checkSealed();
        this.distribution = (AttributesTimeSeriesDistribution) distribution;
    }
}

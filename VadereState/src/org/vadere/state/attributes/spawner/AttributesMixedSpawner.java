package org.vadere.state.attributes.spawner;

import org.vadere.state.attributes.distributions.AttributesDistribution;

import java.util.ArrayList;
import java.util.List;

public class AttributesMixedSpawner extends AttributesSpawner {

    private List<Double> switchpoints;
    private List<AttributesDistribution> distributions;

    public List<Double> getSwitchpoints() {
        return switchpoints;
    }

    public void setSwitchpoints(List<Double> switchpoints) {
        this.switchpoints = switchpoints;
    }

    public List<AttributesDistribution> getDistributions() {
        return distributions;
    }

    public void setDistributions(ArrayList<AttributesDistribution> distributions) {
        this.distributions = distributions;
    }

}

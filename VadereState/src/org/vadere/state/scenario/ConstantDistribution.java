package org.vadere.state.scenario;

import org.apache.commons.math3.distribution.ConstantRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import java.util.List;


/**
 * "Constant" distribution for <code>interSpawnTimeDistribution</code> of
 * {@link org.vadere.state.attributes.scenario.AttributesSource}.
 *
 */
public class ConstantDistribution implements SpawnDistribution {

    private static final long serialVersionUID = 1L;

    private int spawnNumber;
    private int outstandingAgents;
    private double updateFrequency;


    /** Uniform constructor interface: RandomGenerator unusedRng, double... distributionParams */
    public ConstantDistribution(RandomGenerator rng, int spawnNumber, List<Double> distributionParameters) {

        //rng is not required, everything is deterministic
        this.spawnNumber = spawnNumber;
        this.outstandingAgents = 0;
        this.updateFrequency = distributionParameters.get(0);

        // Only a single parameter is required and read for ConstantDistributionLegacy
        assert distributionParameters.size() == 1;
    }

    public int getSpawnNumber() {
        return spawnNumber;
    }

    public double getUpdateFrequency(){
        return updateFrequency;
    }

    @Override
    public int getOutstandingSpawnNumber() {
        return this.outstandingAgents;
    }

    @Override
    public void setOutstandingAgents(int outstandingAgents){
        this.outstandingAgents = outstandingAgents;
    }

    @Override
    public int getSpawnNumber(double timeCurrentEvent) {
        return spawnNumber;
    }

    @Override
    public double getNextSpawnTime(double timeCurrentEvent) {
        //always add a constant value to the 'value'
        return timeCurrentEvent + this.updateFrequency;
    }




}

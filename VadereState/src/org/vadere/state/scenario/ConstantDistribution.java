package org.vadere.state.scenario;

import org.apache.commons.math3.distribution.ConstantRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import java.util.List;




/**
 * "Constant" distribution for <code>interSpawnTimeDistribution</code> of
 * {@link org.vadere.state.attributes.scenario.AttributesSource}.
 *
 */
public class ConstantDistribution extends ConstantRealDistribution implements SpawnDistribution {

    private static final long serialVersionUID = 1L;

    private int spawnNumber;

    /** Uniform constructor interface: RandomGenerator unusedRng, double... distributionParams */
    public ConstantDistribution(RandomGenerator rng, int spawnNumber, List<Double> distributionParameters) {
        super(distributionParameters.get(0));

        //rng is not required, everything is deterministic
        this.spawnNumber = spawnNumber;

        // Only a single parameter is required and read for ConstantDistributionLegacy
        assert distributionParameters.size() == 1;
    }

    @Override
    public int getSpawnNumber(double simTimeInSec) {
        return spawnNumber;
    }

    @Override
    public double getNextSpawnTime(double simTimeInSec) {
        //always add a constant value to the 'value'
        return simTimeInSec + this.sample();
    }

}

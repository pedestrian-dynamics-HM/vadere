package org.vadere.state.scenario;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public class NegativeExponentialDistribution extends ExponentialDistribution {

    /**
     *
     * @param mean inter-arrival time
     */
    public NegativeExponentialDistribution(RandomGenerator rng, double mean) {
        super(rng, mean);
    }
}

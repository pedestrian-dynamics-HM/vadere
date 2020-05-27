package org.vadere.state.scenario;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * -> Scope: Definition of spawn generation in source
 * Use the PoissonDistribution to generate agents with a Poisson process.
 * The Poisson process is a random process.
 * Hence, the generation time of a specific agent is independent from other generation times.
 * The Poisson distribution and the exponential distribution are related.
 * The reciprocal value of the Poisson parameter is the mean inter arrival time used in the exponential distribution.

 * @author  Christina Mayr
 * @since   2020-05-27
 */


public class PoissonDistribution extends ExponentialDistribution {
    /**
     *
     * @param numberPedsPerSecond Unit: agents/second. Reciprocal value of mean inter-arrival time [seconds].
     */

    public PoissonDistribution(RandomGenerator rng, double numberPedsPerSecond) {
        super(rng, 1/numberPedsPerSecond);
    }
}

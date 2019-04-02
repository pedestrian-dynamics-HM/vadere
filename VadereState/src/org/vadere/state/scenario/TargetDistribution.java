package org.vadere.state.scenario;

import java.util.List;
import java.util.Random;

public class TargetDistribution {


    private final Random random;

    public TargetDistribution(Random random) {
        this.random = random;

    }

    public List<Integer> returnTargets(List<List<Integer>> targetIds, List<Double> distribution) {

        double randomNumber = random.nextDouble();
        double tmpSum = 0;

        if (distribution.size() > 0) {
            // throw new RuntimeException("Distribution parameter from SourceAttributes cannot be empty.");
            for (int i = 0; i < distribution.size(); i++) {
                tmpSum += distribution.get(i);
                if (randomNumber < tmpSum) {
                    return targetIds.get(i);
                }
            }
        }

        return null;
    }
}

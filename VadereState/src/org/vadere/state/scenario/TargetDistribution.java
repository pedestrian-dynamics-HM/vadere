package org.vadere.state.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TargetDistribution {



    public TargetDistribution(){

    }

    private double randomCalc() {

        Random rand = new Random();
        return rand.nextDouble();
    }


    public List<Integer> returnTargets(List<List<Integer>> targetIds, List<Double> distribution) {

       double randomNumber = randomCalc();

        double tmpSum = 0;
        for (int i = 0; i <= distribution.size(); i++) {
            tmpSum += distribution.get(i);
            if (randomNumber < tmpSum) {
                return targetIds.get(i);
            }
        }

        return null;
    }
    //targetIdSample: [ [ 10, 20, 21 ], [ 11, 1, 2 ], [ 32,1] ],
    //distributionParameters : [ 0.3, 0.1, 0.6 ] }
    //für eine UniformDistribution würde das bedeutet mit WSK 0.3 erhalten Pedestrians Liste [ 10, 20, 21 ] mit WSK 0.1 [ 11, 1, 2 ] und mit WSK 0.6 [ 32,1].

}

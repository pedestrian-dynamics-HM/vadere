
/*
package org.vadere.simulator.control;

import org.junit.Test;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.state.attributes.scenario.SourceTestAttributesBuilder;
import org.vadere.state.scenario.NegativeExponentialDistribution;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestSourceControllerUsingNegativeExponentialSpawnRate extends TestSourceController {


    @Test
    public void testSpawnTimeDist() throws IOException{
        int timeSteps = 10000;
        double meanIterArrival = 0.35;
        double totalTime = timeSteps* 0.4*meanIterArrival/0.2;
        SourceTestAttributesBuilder builder =
                new SourceTestAttributesBuilder()
                        .setRandomSeed(55)
                        .setStartTime(0.0)
                        .setEndTime(totalTime)
                        .setDistributionClass(NegativeExponentialDistribution.class)
                        .setDistributionParams(meanIterArrival);
        initialize(builder);
        SourceController s = first().sourceController;
        double sumPeds = 0;
        for (double t = 0; t < totalTime; t+=0.4) {
            s.update(t);
            double ped = countPedestriansAndRemove(0);
            sumPeds += ped;
        }

        double arrivalRatePerSec = sumPeds / totalTime;
        assertEquals(meanIterArrival, 1/arrivalRatePerSec,0.1);
    }
}
*/
package org.vadere.state.scenario;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.Arrays;

public class LinearInterpolationSpawnDistribution implements SpawnDistribution {

    private LinearInterpolator interpolator;
    private double timeLastEvent = 0;
    private double spawnFrequency = 0.1;


    public LinearInterpolationSpawnDistribution(RandomGenerator rng){

        double[] xValues = {0., 200., 400.};
        double[] yValues = {0., 200., 0.};

        this.interpolator = new LinearInterpolator();
        this.interpolator.interpolate(xValues, yValues);
    }


    @Override
    public int getSpawnNumber(double timeCurrentEvent){
        double timeDifference = timeCurrentEvent - timeLastEvent;
    }

    @Override
    public double getNextSpawnTime(double timeCurrentEvent) {
        return timeCurrentEvent + spawnFrequency;
    }

}

package org.vadere.state.scenario;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.util.math.TruncatedNormalDistribution;

import java.util.List;


public class LinearInterpolationSpawnDistribution implements SpawnDistribution {

    private PolynomialSplineFunction interpolator;
    private double timeLastEvent = 0;
    private double spawnFrequency;
    private TruncatedNormalDistribution truncNormalDist;
    private RandomGenerator randomGenerator;


    public LinearInterpolationSpawnDistribution(RandomGenerator rng, List<Double> distributionParameters){

        // TODO: check that xValues are in order
        // TODO: check that yValues are integers and strictly non-negative

        double[] xValues = {0., 200., 400., 600, 800};
        double[] yValues = {0., 8., 0., 12, 0};
        //double[] yValues = {1., 1., 0., 15, 0};

        this.spawnFrequency = distributionParameters.get(0);
        this.interpolator = new LinearInterpolator().interpolate(xValues, yValues);

        this.randomGenerator = rng;
        this.truncNormalDist = new TruncatedNormalDistribution(
                this.randomGenerator, 0, 1, -4, 4, 1000);

    }

    @Override
    public int getSpawnNumber(double timeCurrentEvent){
        int spawnNumber = (int) Math.round(this.interpolator.value(timeCurrentEvent) + this.truncNormalDist.sample());
        spawnNumber = Math.max(0, spawnNumber);

        System.out.println("timeCurrentEvent " + timeCurrentEvent + " spawn number = " + spawnNumber);

        return spawnNumber;
    }

    @Override
    public double getNextSpawnTime(double timeCurrentEvent) {
        return timeCurrentEvent + spawnFrequency;
    }

}

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
    private int outstandingAgents = 0;


    public LinearInterpolationSpawnDistribution(RandomGenerator rng, List<Double> distributionParameters){

        // Most correctness is checked in LinearInterpolator
        double[] xValues = {0., 200., 400, 600, 700, 800};
        double[] yValues = {3., 8., 2., 10, 0, 0};

        for(double d : yValues){
            if(d < 0){
                throw new IllegalArgumentException("No negative values are allowed for yValues. Got " + d);
            }
        }

        this.spawnFrequency = distributionParameters.get(0);
        this.interpolator = new LinearInterpolator().interpolate(xValues, yValues);

        this.randomGenerator = rng;

        //https://www.wolframalpha.com/input/?i=normal+distribution%2C+mean%3D0%2C+sd%3D3+from+-4+to+4
        this.truncNormalDist = new TruncatedNormalDistribution(
                this.randomGenerator, 0, 3, -4, 4, 1000);

    }

    @Override
    public int getSpawnNumber(double timeCurrentEvent){
        int spawnNumber = (int) Math.round(this.interpolator.value(timeCurrentEvent) + this.truncNormalDist.sample());
        spawnNumber = Math.max(0, spawnNumber);

        //System.out.println("timeCurrentEvent " + timeCurrentEvent + " spawn number = " + spawnNumber);

        return spawnNumber;
    }

    @Override
    public int getOutstandingSpawnNumber(){
        // Agents that could not be spawned (e.g. because the Source is too small) are not taken to the next update
        return 0;
    }

    @Override
    public double getNextSpawnTime(double timeCurrentEvent) {
        return timeCurrentEvent + spawnFrequency;
    }



    @Override
    public void setOutstandingAgents(int outstandingAgents) {
    }
}

package org.vadere.state.scenario.spawner.impl;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.scenario.Topography;
import org.vadere.state.scenario.distribution.impl.LinearInterpolationDistribution;
import org.vadere.util.math.TruncatedNormalDistribution;

import java.util.Random;

public class LinearInterpolationSpawner extends RegularSpawner {
    private TruncatedNormalDistribution truncNormalDist;
    private PolynomialSplineFunction interpolator;

    public LinearInterpolationSpawner(AttributesSpawner attributes) {
        super(attributes);

        double[] xValues = { 0, 200, 400, 600, 800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400 };
        double[] yValues = { -1., 8., -1., 8., -1.0, 8., -1, 8, -1, 8, -1, 8, -1 };

        for (double d : yValues) {
            if (d < -1) {
                throw new IllegalArgumentException("No negative values are allowed for yValues. Got " + d);
            }
        }

        this.interpolator = new LinearInterpolator().interpolate(xValues, yValues);

        // https://www.wolframalpha.com/input/?i=normal+distribution%2C+mean%3D0%2C+sd%3D3+from+-4+to+4
        this.truncNormalDist = new TruncatedNormalDistribution(new JDKRandomGenerator(new Random().nextInt()), 0, 3, -4, 4, 1000);
    }



    @Override
    public int getSpawnNumber(double timeCurrentEvent) {
        int spawnNumber = (int) Math.round(this.interpolator.value(timeCurrentEvent) + this.truncNormalDist.sample());
        spawnNumber = Math.max(0, spawnNumber);
        return spawnNumber;
    }

    @Override
    public int getRemainingSpawnAgents() {
        // Agents that could not be spawned (e.g. because the source is too small) are
        // not taken to the next update
        return 0;
    }

    @Override
    public void update(double simTimeInSec) {

    }

    @Override
    public void setRemainingSpawnAgents(int remainingAgents) {
        // do nothing
    }

    @Override
    protected boolean isQueueEmpty() {
        return false;
    }
}

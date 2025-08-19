package org.vadere.state.scenario.spawner.impl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.vadere.state.attributes.distributions.*;
import org.vadere.state.attributes.spawner.AttributesRegularSpawner;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.scenario.SpawnerFactory;
import org.vadere.state.scenario.distribution.impl.SingleSpawnDistribution;
import org.vadere.state.scenario.spawner.VSpawner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class RegularSpawnerTest {


    @Test
    void testConstantDistribution() {
        AttributesConstantDistribution attributesDistribution = new AttributesConstantDistribution();
        attributesDistribution.setUpdateFrequency(1.);

        testElementConstraintSpawner(attributesDistribution);
        testTimeConstraintSpawner(attributesDistribution);
    }

    @Test
    void testBinomialDistribution() {
        AttributesBinomialDistribution attributesDistribution = new AttributesBinomialDistribution();
        attributesDistribution.setP(1.);
        attributesDistribution.setTrials(1);

        testElementConstraintSpawner(attributesDistribution);
        testTimeConstraintSpawner(attributesDistribution);
    }

    @Test
    void testEmpiricalDistribution() {
        AttributesEmpiricalDistribution attributesDistribution = new AttributesEmpiricalDistribution();
        attributesDistribution.setValues(List.of(1.));

        testElementConstraintSpawner(attributesDistribution);
        testTimeConstraintSpawner(attributesDistribution);
    }



    @Test
    void testNegativeExponentialDistribution() {
        AttributesNegativeExponentialDistribution attributesDistribution = new AttributesNegativeExponentialDistribution();
        attributesDistribution.setMean(1);

        testElementConstraintSpawner(attributesDistribution);
        testTimeConstraintSpawner(attributesDistribution);
    }

    @Test
    void testNormalDistribution() {
        AttributesNormalDistribution attributesDistribution = new AttributesNormalDistribution();
        attributesDistribution.setMean(1);
        attributesDistribution.setSd(0.1);

        testElementConstraintSpawner(attributesDistribution);
        testTimeConstraintSpawner(attributesDistribution);
    }

    @Test
    void testPoissonDistribution() {
        AttributesPoissonDistribution attributesDistribution = new AttributesPoissonDistribution();
        attributesDistribution.setNumberPedsPerSecond(1);

        testElementConstraintSpawner(attributesDistribution);
        testTimeConstraintSpawner(attributesDistribution);
    }

    @Test
    void testLinearInterpolationDistribution() {
        AttributesLinearInterpolationDistribution attributesDistribution = new AttributesLinearInterpolationDistribution();
        attributesDistribution.setSpawnFrequency(1.);

        testElementConstraintSpawner(attributesDistribution);
        testTimeConstraintSpawner(attributesDistribution);
    }

    @Test
    void testSingleSpawnDistribution() {
        AttributesSingleSpawnDistribution attributesDistribution = new AttributesSingleSpawnDistribution();
        attributesDistribution.setSpawnTime(3.);

        testElementConstraintSpawner(attributesDistribution);
        testTimeConstraintSpawner(attributesDistribution);
    }

    @Test
    void testTimeSeriesDistribution() {
        AttributesTimeSeriesDistribution attributesDistribution = new AttributesTimeSeriesDistribution();
        attributesDistribution.setSpawnsPerInterval(new ArrayList<>(List.of(1, 0)));
        attributesDistribution.setIntervalLength(1);

        testTimeConstraintSpawner(attributesDistribution);
    }

    @Test
    void testMixedDistribution() {
        AttributesMixedDistribution attributesDistribution = new AttributesMixedDistribution();
        double updateFrequency1 = 1.;
        double updateFrequency2 = 2.;
        attributesDistribution.setDistributions(new ArrayList<>(List.of(new AttributesConstantDistribution(updateFrequency1), new AttributesConstantDistribution(updateFrequency2))));
        attributesDistribution.setSwitchpoints(new ArrayList<>(List.of(3.)));
        VSpawner<?> elementConstraintSpawner = createElementConstraintSpawner(attributesDistribution);

        assertTimeout(Duration.ofSeconds(1), () -> {
            double simTime = elementConstraintSpawner.getAttributes().getConstraintsTimeStart();
            double prevSimTime = simTime;

            while (!elementConstraintSpawner.isFinished(simTime, () -> true)) {
                simTime = elementConstraintSpawner.getNextSpawnTime(simTime);
                if (simTime <= attributesDistribution.getSwitchpoints().get(0)) {
                    assertEquals(updateFrequency1, simTime - prevSimTime);
                } else {
                    assertEquals(updateFrequency2, simTime - prevSimTime);
                }
                elementConstraintSpawner.incrementElementsCreatedTotal(elementConstraintSpawner.getEventElementCount(simTime));
                prevSimTime = simTime;
            }
        });
    }


    void testElementConstraintSpawner(AttributesDistribution attributesDistribution) {
        VSpawner<?> spawner = createElementConstraintSpawner(attributesDistribution);

        assertTimeout(Duration.ofSeconds(1), () -> {
            double simTime = 0;
            double stepTime = 0.4;
            double timeOfNextEvent = spawner.getNextSpawnTime(simTime);
            int total_spawns = 0;
                while (simTime <= spawner.getAttributes().getConstraintsTimeEnd()) {
                    if (!spawner.isFinished(simTime, () -> true)) {
                        spawner.incrementElementsCreatedTotal(spawner.getEventElementCount(simTime));
                        total_spawns += spawner.getEventElementCount(timeOfNextEvent);
                        timeOfNextEvent = spawner.getNextSpawnTime(simTime);
                    }
                    simTime += stepTime;
                }
            assertEquals(spawner.getAttributes().getConstraintsElementsMax(), total_spawns);
        });
    }

    void testTimeConstraintSpawner(AttributesDistribution attributesDistribution) {
        VSpawner<?> spawner = createTimeConstraintSpawner(attributesDistribution);

        assertTimeout(Duration.ofSeconds(1), () -> {
            double simTime = 0;
            double stepTime = 0.4;
            double timeOfNextEvent = spawner.getNextSpawnTime(simTime);
            double prevTimeOfNextEvent = timeOfNextEvent;

            while (simTime <= spawner.getAttributes().getConstraintsTimeEnd()) {
                if (!spawner.isFinished(simTime, () -> true)) {
                    while (timeOfNextEvent <= simTime) {

                        spawner.incrementElementsCreatedTotal(spawner.getEventElementCount(simTime));
                        timeOfNextEvent = spawner.getNextSpawnTime(simTime);
                        assertTrue(timeOfNextEvent >= prevTimeOfNextEvent);
                        if (!(spawner.getDistribution() instanceof SingleSpawnDistribution)) {
                            assertTrue(timeOfNextEvent >= simTime);
                        } else {
                            break;
                        }
                        prevTimeOfNextEvent = timeOfNextEvent;
                    }
                }
                simTime += stepTime;
            }
        });

    }


    private VSpawner<?> createTimeConstraintSpawner(AttributesDistribution attributesDistribution) {
        AttributesSpawner attributesSpawner = createSpawnerWithDefaultValues();
        attributesSpawner.setDistributionAttributes(attributesDistribution);

        attributesSpawner.setConstraintsTimeStart(2.);
        attributesSpawner.setConstraintsTimeEnd(100.);

        return SpawnerFactory.create(attributesSpawner, new Random(0));
    }

    private VSpawner<?> createElementConstraintSpawner(AttributesDistribution attributesDistribution) {
        AttributesSpawner attributesSpawner = createSpawnerWithDefaultValues();
        attributesSpawner.setDistributionAttributes(attributesDistribution);
        attributesSpawner.setConstraintsTimeStart(2.);
        attributesSpawner.setConstraintsTimeEnd(100.);

        attributesSpawner.setConstraintsElementsMax(4);

        return SpawnerFactory.create(attributesSpawner, new Random(0));
    }

    private AttributesSpawner createSpawnerWithDefaultValues() {
        AttributesSpawner attributesSpawner = new AttributesRegularSpawner();
        attributesSpawner.setEventPositionRandom(false);
        attributesSpawner.setEventPositionGridCA(false);
        attributesSpawner.setEventPositionFreeSpace(false);
        attributesSpawner.setEventElementCount(1);
        return attributesSpawner;
    }

}
package org.vadere.state.scenario.spawner.impl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.vadere.state.attributes.distributions.*;
import org.vadere.state.attributes.spawner.AttributesRegularSpawner;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.scenario.SpawnerFactory;
import org.vadere.state.scenario.spawner.VSpawner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class RegularSpawnerTest {


    @Test
    void testConstantDistribution() {
        AttributesConstantDistribution attributesDistribution = new AttributesConstantDistribution();
        attributesDistribution.setUpdateFrequency(1.);

        VSpawner<?> elementConstraintSpawner = getSpawner(attributesDistribution, true);
        testElementConstraintSpawner(elementConstraintSpawner);

        VSpawner<?> timeConstraintSpawner = getSpawner(attributesDistribution, false);
        testTimeConstraintSpawner(timeConstraintSpawner);
    }

    @Test
    void testBinomialDistribution() {
        AttributesBinomialDistribution attributesDistribution = new AttributesBinomialDistribution();
        attributesDistribution.setP(1.);
        attributesDistribution.setTrials(1);

        VSpawner<?> elementConstraintSpawner = getSpawner(attributesDistribution, true);
        testElementConstraintSpawner(elementConstraintSpawner);

        VSpawner<?> timeConstraintSpawner = getSpawner(attributesDistribution, false);
        testTimeConstraintSpawner(timeConstraintSpawner);
    }

    @Test
    void testEmpiricalDistribution() {
        AttributesEmpiricalDistribution attributesDistribution = new AttributesEmpiricalDistribution();
        attributesDistribution.setValues(List.of(1.));

        VSpawner<?> elementConstraintSpawner = getSpawner(attributesDistribution, true);
        testElementConstraintSpawner(elementConstraintSpawner);

        VSpawner<?> timeConstraintSpawner = getSpawner(attributesDistribution, false);
        testTimeConstraintSpawner(timeConstraintSpawner);
    }



    @Test
    void testNegativeExponentialDistribution() {
        AttributesNegativeExponentialDistribution attributesDistribution = new AttributesNegativeExponentialDistribution();
        attributesDistribution.setMean(1);

        VSpawner<?> elementConstraintSpawner = getSpawner(attributesDistribution, true);
        testElementConstraintSpawner(elementConstraintSpawner);

        VSpawner<?> timeConstraintSpawner = getSpawner(attributesDistribution, false);
        testTimeConstraintSpawner(timeConstraintSpawner);
    }

    @Test
    void testNormalDistribution() {
        AttributesNormalDistribution attributesDistribution = new AttributesNormalDistribution();
        attributesDistribution.setMean(1);
        attributesDistribution.setSd(0.1);

        VSpawner<?> elementConstraintSpawner = getSpawner(attributesDistribution, true);
        testElementConstraintSpawner(elementConstraintSpawner);

        VSpawner<?> timeConstraintSpawner = getSpawner(attributesDistribution, false);
        testTimeConstraintSpawner(timeConstraintSpawner);
    }

    @Test
    void testPoissonDistribution() {
        AttributesPoissonDistribution attributesDistribution = new AttributesPoissonDistribution();
        attributesDistribution.setNumberPedsPerSecond(1);

        VSpawner<?> elementConstraintSpawner = getSpawner(attributesDistribution, true);
        testElementConstraintSpawner(elementConstraintSpawner);

        VSpawner<?> timeConstraintSpawner = getSpawner(attributesDistribution, false);
        testTimeConstraintSpawner(timeConstraintSpawner);
    }

    @Test
    void testLinearInterpolationDistribution() {
        AttributesLinearInterpolationDistribution attributesDistribution = new AttributesLinearInterpolationDistribution();
        attributesDistribution.setSpawnFrequency(1.);

        VSpawner<?> elementConstraintSpawner = getSpawner(attributesDistribution, true);
        testElementConstraintSpawner(elementConstraintSpawner);

        VSpawner<?> timeConstraintSpawner = getSpawner(attributesDistribution, false);
        testTimeConstraintSpawner(timeConstraintSpawner);
    }

    @Test
    void testSingleSpawnDistribution() {
        AttributesSingleSpawnDistribution attributesDistribution = new AttributesSingleSpawnDistribution();
        attributesDistribution.setSpawnTime(2.);

        VSpawner<?> elementConstraintSpawner = getSpawner(attributesDistribution, true);
        testElementConstraintSpawner(elementConstraintSpawner);

        VSpawner<?> timeConstraintSpawner = getSpawner(attributesDistribution, false);
        testElementConstraintSpawner(timeConstraintSpawner);
    }

    @Test
    void testTimeSeriesDistribution() {
        AttributesTimeSeriesDistribution attributesDistribution = new AttributesTimeSeriesDistribution();
        attributesDistribution.setSpawnsPerInterval(new ArrayList<>(List.of(1)));
        attributesDistribution.setIntervalLength(1);
        VSpawner<?> elementConstraintSpawner = getSpawner(attributesDistribution, true);

        testElementConstraintSpawner(elementConstraintSpawner);
    }

    @Test
    void testMixedDistribution() {
        AttributesMixedDistribution attributesDistribution = new AttributesMixedDistribution();
        attributesDistribution.setDistributions(new ArrayList<>(List.of(new AttributesConstantDistribution(1.), new AttributesConstantDistribution(2.))));
        attributesDistribution.setSwitchpoints(new ArrayList<>(List.of(3.)));
        VSpawner<?> elementConstraintSpawner = getSpawner(attributesDistribution, true);

        double simTime = elementConstraintSpawner.getAttributes().getConstraintsTimeStart();

        while(!elementConstraintSpawner.isFinished(simTime, () -> true)) {
            simTime = elementConstraintSpawner.getNextSpawnTime(simTime);
            elementConstraintSpawner.incrementElementsCreatedTotal(elementConstraintSpawner.getEventElementCount(simTime));
            System.out.println(simTime);
        }
    }


    void testElementConstraintSpawner(VSpawner<?> spawner) {
        double simTime = 0;
        double stepTime = 0.4;
        double timeOfNextEvent = spawner.getNextSpawnTime(simTime);
        int spawns = 0;

        while(simTime <= spawner.getAttributes().getConstraintsTimeEnd()) {
            if (!spawner.isFinished(simTime, () -> true)) {
                while (timeOfNextEvent <= simTime) {
                    System.out.println("test");
                    spawner.incrementElementsCreatedTotal(spawner.getEventElementCount(simTime));
                    spawns += spawner.getEventElementCount(timeOfNextEvent);
                    timeOfNextEvent = spawner.getNextSpawnTime(simTime);
                }
            }
            simTime += stepTime;
        }

        assertEquals(spawner.getAttributes().getConstraintsElementsMax(), spawns);
    }

    void testTimeConstraintSpawner(VSpawner<?> spawner) {
        double simTime = 0;
        double stepTime = 0.4;
        double timeOfNextEvent = spawner.getNextSpawnTime(simTime);

        while(simTime <= spawner.getAttributes().getConstraintsTimeEnd()) {
            if (!spawner.isFinished(simTime, () -> true)) {
                while (timeOfNextEvent <= simTime) {

                    spawner.incrementElementsCreatedTotal(spawner.getEventElementCount(simTime));
                    timeOfNextEvent = spawner.getNextSpawnTime(simTime);
                    assertTrue(timeOfNextEvent >= simTime);
                }
            }
            simTime += stepTime;
        }

    }


    private VSpawner<?> getSpawner(AttributesDistribution attributesDistribution, boolean elementConstraint) {
        AttributesSpawner attributesSpawner = new AttributesRegularSpawner();
        attributesSpawner.setDistributionAttributes(attributesDistribution);
        if (elementConstraint) {
            attributesSpawner.setConstraintsElementsMax(4);
        }
        attributesSpawner.setConstraintsTimeStart(2.);
        attributesSpawner.setConstraintsTimeEnd(100.);
        attributesSpawner.setEventPositionRandom(false);
        attributesSpawner.setEventPositionGridCA(false);
        attributesSpawner.setEventPositionFreeSpace(false);
        attributesSpawner.setEventElementCount(1);
        return SpawnerFactory.create(attributesSpawner, new Random(0));
    }
}